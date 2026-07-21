package com.tradernet.api.resources;

import com.tradernet.marketai.MarketAiService;
import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.api.ApiConfiguration;
import com.tradernet.user.AuthSessionService;
import com.tradernet.user.AuthenticationAuditService;
import com.tradernet.user.AuthorizationService;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * WebSocket endpoint streaming bar updates and AI signals.
 */
@ServerEndpoint(value = "/ws/market", configurator = MarketStreamEndpoint.AuthenticatedConfigurator.class)
public class MarketStreamEndpoint {

    private static final String SESSION_ID_PROPERTY = "tradernet.sessionId";
    private static final String ORIGIN_PROPERTY = "tradernet.websocket.origin";
    private static final String REQUEST_URI_PROPERTY = "tradernet.websocket.requestUri";
    private static final String REQUEST_HOST_PROPERTY = "tradernet.websocket.requestHost";

    private AutoCloseable barSubscription;
    private AutoCloseable signalSubscription;
    private MarketStreamDeliveryService deliveryService;
    private MarketWebSocketSessionRegistry sessionRegistry;
    private Session session;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        String sessionId = (String) config.getUserProperties().get(SESSION_ID_PROPERTY);
        final ApiConfiguration apiConfiguration = CDI.current().select(ApiConfiguration.class).get();
        final String origin = (String) config.getUserProperties().get(ORIGIN_PROPERTY);
        final String requestUri = (String) config.getUserProperties().get(REQUEST_URI_PROPERTY);
        final String requestHost = (String) config.getUserProperties().get(REQUEST_HOST_PROPERTY);
        if (!apiConfiguration.isWebSocketOriginAllowed(origin, requestUri, requestHost)) {
            auditService().record("websocket", "rejected", null, null, "origin_not_allowed");
            closePolicyViolation(session, "Origin not allowed");
            return;
        }

        final AuthSessionService authSessionService = CDI.current().select(AuthSessionService.class).get();
        final Optional<AuthUserDto> authUser = authSessionService.getSessionUser(sessionId);
        if (authUser.isEmpty()) {
            auditService().record("websocket", "rejected", null, null, "not_authenticated");
            closeUnauthenticated(session);
            return;
        }

        final AuthorizationService authorizationService = CDI.current().select(AuthorizationService.class).get();
        final Set<String> requiredRoles = authorizationService.getRequiredRoles("GET", "market");
        if (requiredRoles.isEmpty() || !authorizationService.hasAnyRole(authUser.get(), requiredRoles)) {
            auditService().record(
                "websocket",
                "rejected",
                authUser.get().getUsername(),
                null,
                requiredRoles.isEmpty() ? "policy_missing" : "insufficient_role"
            );
            closePolicyViolation(session, "Market access denied");
            return;
        }

        final MarketAiService service = CDI.current().select(MarketAiService.class).get();
        deliveryService = CDI.current().select(MarketStreamDeliveryService.class).get();
        sessionRegistry = CDI.current().select(MarketWebSocketSessionRegistry.class).get();
        this.session = session;
        sessionRegistry.register(session, sessionId, authUser.get());
        deliveryService.register(session);
        final String requestedCurrency = session.getRequestParameterMap().getOrDefault("currency", List.of("USD")).stream().findFirst().orElse("USD");
        final String requestedSymbol = session.getRequestParameterMap().getOrDefault("symbol", List.of("BTCUSDT")).stream().findFirst().orElse("BTCUSDT");
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(requestedSymbol);
        service.ensureLiveSymbol(normalizedSymbol);
        barSubscription = service.subscribeBars(bar -> {
            if (matchesSymbol(bar.getSymbol(), normalizedSymbol)) {
                deliveryService.enqueueBar(session, bar, requestedCurrency);
            }
        });
        signalSubscription = service.subscribeSignals(signal -> {
            if (matchesSymbol(signal.getSymbol(), normalizedSymbol)) {
                deliveryService.enqueueSignal(session, signal);
            }
        });
    }

    @OnClose
    public void onClose() {
        cleanup();
    }

    @OnError
    public void onError(Throwable error) {
        cleanup();
    }

    private boolean matchesSymbol(String actualSymbol, String expectedSymbol) {
        return actualSymbol != null && MarketSymbolNormalizer.normalizeSymbol(actualSymbol).equals(expectedSymbol);
    }

    private void cleanup() {
        closeQuietly(barSubscription);
        closeQuietly(signalSubscription);
        barSubscription = null;
        signalSubscription = null;
        if (deliveryService != null) {
            deliveryService.unregister(session);
        }
        if (sessionRegistry != null) {
            sessionRegistry.unregister(session);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // no-op
        }
    }

    private void closeUnauthenticated(Session session) {
        closePolicyViolation(session, "Not authenticated");
    }

    private AuthenticationAuditService auditService() {
        return CDI.current().select(AuthenticationAuditService.class).get();
    }

    private void closePolicyViolation(Session session, String reason) {
        try {
            session.close(new CloseReason(
                CloseReason.CloseCodes.VIOLATED_POLICY,
                reason
            ));
        } catch (IOException ignored) {
            // The handshake already failed from the client's perspective.
        }
    }

    public static class AuthenticatedConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
            config.getUserProperties().remove(SESSION_ID_PROPERTY);
            config.getUserProperties().remove(ORIGIN_PROPERTY);
            config.getUserProperties().remove(REQUEST_HOST_PROPERTY);
            config.getUserProperties().put(REQUEST_URI_PROPERTY, request.getRequestURI().toString());
            final String origin = firstHeader(request.getHeaders(), "Origin");
            final String host = firstHeader(request.getHeaders(), "Host");
            if (origin != null) {
                config.getUserProperties().put(ORIGIN_PROPERTY, origin);
            }
            if (host != null) {
                config.getUserProperties().put(REQUEST_HOST_PROPERTY, host);
            }
            String sessionId = findSessionId(request.getHeaders());
            if (sessionId != null) {
                config.getUserProperties().put(SESSION_ID_PROPERTY, sessionId);
            }
        }

        private String firstHeader(Map<String, List<String>> headers, String name) {
            for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                if (name.equalsIgnoreCase(header.getKey()) && !header.getValue().isEmpty()) {
                    return header.getValue().get(0);
                }
            }
            return null;
        }

        private String findSessionId(Map<String, List<String>> headers) {
            for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                if (!"Cookie".equalsIgnoreCase(header.getKey())) {
                    continue;
                }

                for (String cookieHeader : header.getValue()) {
                    String sessionId = findCookieValue(cookieHeader, AuthResource.SESSION_COOKIE_NAME);
                    if (sessionId != null) {
                        return sessionId;
                    }
                }
            }
            return null;
        }

        private String findCookieValue(String cookieHeader, String name) {
            if (cookieHeader == null || cookieHeader.isBlank()) {
                return null;
            }

            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && name.equals(parts[0])) {
                    return parts[1];
                }
            }
            return null;
        }
    }
}
