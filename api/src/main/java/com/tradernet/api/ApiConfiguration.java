package com.tradernet.api;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validated API transport configuration.
 */
@Singleton
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ApiConfiguration {

    private static final int DEFAULT_MAX_PENDING_EVENTS = 128;

    private boolean secureCookies;
    private int maxPendingWebsocketEvents;
    private Set<String> allowedWebSocketOrigins;

    @PostConstruct
    void load() {
        secureCookies = booleanValue(
            "tradernet.auth.cookie.secure",
            System.getProperty("tradernet.auth.cookie.secure"),
            System.getenv("TRADERNET_AUTH_COOKIE_SECURE"),
            true
        );
        maxPendingWebsocketEvents = intValue(
            "market.ai.websocket.maxPendingEvents",
            DEFAULT_MAX_PENDING_EVENTS,
            1,
            10_000
        );
        allowedWebSocketOrigins = originSet(firstNonBlank(
            System.getProperty("tradernet.auth.websocket.allowedOrigins"),
            System.getenv("TRADERNET_AUTH_WEBSOCKET_ALLOWED_ORIGINS")
        ));
    }

    public boolean useSecureCookies() {
        return secureCookies;
    }

    public int getMaxPendingWebsocketEvents() {
        return maxPendingWebsocketEvents;
    }

    public boolean isWebSocketOriginAllowed(String origin, String requestUri, String requestHost) {
        final String normalizedOrigin = normalizeOrigin(origin);
        if (normalizedOrigin == null) {
            return false;
        }
        if (!allowedWebSocketOrigins.isEmpty()) {
            return allowedWebSocketOrigins.contains(normalizedOrigin);
        }

        final String requestOrigin = requestOrigin(requestUri, requestHost, normalizedOrigin);
        return normalizedOrigin.equals(requestOrigin);
    }

    private Set<String> originSet(String configuredOrigins) {
        if (configuredOrigins == null) {
            return Set.of();
        }
        return Arrays.stream(configuredOrigins.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .map(value -> {
                final String normalized = normalizeOrigin(value);
                if (normalized == null) {
                    throw new IllegalStateException("Invalid WebSocket origin: " + value);
                }
                return normalized;
            })
            .collect(Collectors.toUnmodifiableSet());
    }

    private String requestOrigin(String requestUri, String requestHost, String normalizedOrigin) {
        try {
            if (requestUri != null && !requestUri.isBlank()) {
                final URI uri = new URI(requestUri);
                final String scheme = httpScheme(uri.getScheme());
                if (scheme != null && uri.getHost() != null) {
                    return normalizedOrigin(scheme, uri.getHost(), uri.getPort());
                }
            }

            if (requestHost == null || requestHost.isBlank()) {
                return null;
            }
            final URI originUri = new URI(normalizedOrigin);
            final URI hostUri = new URI(originUri.getScheme() + "://" + requestHost.trim());
            return normalizedOrigin(originUri.getScheme(), hostUri.getHost(), hostUri.getPort());
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private String httpScheme(String requestScheme) {
        if ("ws".equalsIgnoreCase(requestScheme) || "http".equalsIgnoreCase(requestScheme)) {
            return "http";
        }
        if ("wss".equalsIgnoreCase(requestScheme) || "https".equalsIgnoreCase(requestScheme)) {
            return "https";
        }
        return null;
    }

    private String normalizeOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return null;
        }
        try {
            final URI uri = new URI(origin.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath()))) {
                return null;
            }
            return normalizedOrigin(uri.getScheme(), uri.getHost(), uri.getPort());
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private String normalizedOrigin(String scheme, String host, int port) {
        if (scheme == null || host == null) {
            return null;
        }
        final String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        final int defaultPort = "https".equals(normalizedScheme) ? 443 : 80;
        final int normalizedPort = port < 0 || port == defaultPort ? -1 : port;
        try {
            return new URI(
                normalizedScheme,
                null,
                host.toLowerCase(Locale.ROOT),
                normalizedPort,
                null,
                null,
                null
            ).toASCIIString();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private int intValue(String key, int fallback, int min, int max) {
        final String configured = System.getProperty(key);
        if (configured == null || configured.isBlank()) {
            return fallback;
        }
        try {
            final int value = Integer.parseInt(configured.trim());
            if (value < min || value > max) {
                throw new IllegalStateException(key + " must be between " + min + " and " + max);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(key + " must be an integer", ex);
        }
    }

    private boolean booleanValue(
        String key,
        String propertyValue,
        String environmentValue,
        boolean fallback
    ) {
        final String configured = firstNonBlank(propertyValue, environmentValue);
        if (configured == null) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(configured)) {
            return true;
        }
        if ("false".equalsIgnoreCase(configured)) {
            return false;
        }
        throw new IllegalStateException(key + " must be true or false");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
