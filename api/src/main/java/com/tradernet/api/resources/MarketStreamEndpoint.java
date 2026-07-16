package com.tradernet.api.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.MarketDataViewService;
import com.tradernet.marketai.MarketSymbolNormalizer;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.user.AuthSessionService;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.websocket.CloseReason;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket endpoint streaming bar updates and AI signals.
 */
@ServerEndpoint(value = "/ws/market", configurator = MarketStreamEndpoint.AuthenticatedConfigurator.class)
public class MarketStreamEndpoint {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SESSION_ID_PROPERTY = "tradernet.sessionId";

    private AutoCloseable barSubscription;
    private AutoCloseable signalSubscription;

    @OnOpen
    public void onOpen(Session session) {
        String sessionId = (String) session.getUserProperties().get(SESSION_ID_PROPERTY);
        final AuthSessionService authSessionService = CDI.current().select(AuthSessionService.class).get();
        if (!authSessionService.hasValidSession(sessionId)) {
            closeUnauthenticated(session);
            return;
        }

        final MarketAiService service = CDI.current().select(MarketAiService.class).get();
        final MarketDataViewService marketDataViewService = CDI.current().select(MarketDataViewService.class).get();
        final String requestedCurrency = session.getRequestParameterMap().getOrDefault("currency", List.of("USD")).stream().findFirst().orElse("USD");
        final String requestedSymbol = session.getRequestParameterMap().getOrDefault("symbol", List.of("BTCUSDT")).stream().findFirst().orElse("BTCUSDT");
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(requestedSymbol);
        service.ensureLiveSymbol(normalizedSymbol);
        barSubscription = service.subscribeBars(bar -> {
            if (matchesSymbol(bar.getSymbol(), normalizedSymbol)) {
                send(session, "bar", marketDataViewService.convertBar(bar, requestedCurrency));
            }
        });
        signalSubscription = service.subscribeSignals(signal -> {
            if (matchesSymbol(signal.getSymbol(), normalizedSymbol)) {
                send(session, "signal", signal);
            }
        });
    }

    @OnClose
    public void onClose() {
        closeQuietly(barSubscription);
        closeQuietly(signalSubscription);
    }

    private void send(Session session, String type, Object payload) {
        final String message = toJson(type, payload);
        if (message == null) {
            return;
        }

        synchronized (session) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(message);
            }
        }
    }

    private String toJson(String type, Object payload) {
        final Map<String, Object> envelope = new HashMap<>();
        envelope.put("type", type);
        envelope.put("payload", payload);
        try {
            return OBJECT_MAPPER.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private boolean matchesSymbol(String actualSymbol, String expectedSymbol) {
        return actualSymbol == null || MarketSymbolNormalizer.normalizeSymbol(actualSymbol).equals(expectedSymbol);
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
