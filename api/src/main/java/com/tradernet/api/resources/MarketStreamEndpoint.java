package com.tradernet.api.resources;

import com.tradernet.marketai.MarketAiService;
import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.user.AuthSessionService;
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

/**
 * WebSocket endpoint streaming bar updates and AI signals.
 */
@ServerEndpoint(value = "/ws/market", configurator = MarketStreamEndpoint.AuthenticatedConfigurator.class)
public class MarketStreamEndpoint {

    private static final String SESSION_ID_PROPERTY = "tradernet.sessionId";

    private AutoCloseable barSubscription;
    private AutoCloseable signalSubscription;
    private MarketStreamDeliveryService deliveryService;
    private Session session;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        String sessionId = (String) config.getUserProperties().get(SESSION_ID_PROPERTY);
        final AuthSessionService authSessionService = CDI.current().select(AuthSessionService.class).get();
        if (!authSessionService.hasValidSession(sessionId)) {
            closeUnauthenticated(session);
            return;
        }

        final MarketAiService service = CDI.current().select(MarketAiService.class).get();
        deliveryService = CDI.current().select(MarketStreamDeliveryService.class).get();
        this.session = session;
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
        try {
            session.close(new CloseReason(
                CloseReason.CloseCodes.VIOLATED_POLICY,
                "Not authenticated"
            ));
        } catch (IOException ignored) {
            // The handshake already failed from the client's perspective.
        }
    }

    public static class AuthenticatedConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
            config.getUserProperties().remove(SESSION_ID_PROPERTY);
            String sessionId = findSessionId(request.getHeaders());
            if (sessionId != null) {
                config.getUserProperties().put(SESSION_ID_PROPERTY, sessionId);
            }
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
