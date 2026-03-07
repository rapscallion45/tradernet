package com.tradernet.api.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.currencyconversion.CurrencyConversionService;
import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.MarketBar;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket endpoint streaming bar updates and AI signals.
 */
@ServerEndpoint("/ws/market")
public class MarketStreamEndpoint {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AutoCloseable barSubscription;
    private AutoCloseable signalSubscription;

    @OnOpen
    public void onOpen(Session session) {
        final MarketAiService service = CDI.current().select(MarketAiService.class).get();
        final CurrencyConversionService conversionService = CDI.current().select(CurrencyConversionService.class).get();
        final String requestedCurrency = session.getRequestParameterMap().getOrDefault("currency", java.util.List.of("USD")).stream().findFirst().orElse("USD");
        final CurrencyCode targetCurrency = CurrencyCode.parseOrDefault(requestedCurrency, CurrencyCode.USD);
        barSubscription = service.subscribeBars(bar -> send(session, "bar", conversionService.convertBar(bar, targetCurrency)));
        signalSubscription = service.subscribeSignals(signal -> send(session, "signal", signal));
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
}
