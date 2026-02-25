package com.tradernet.marketai.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.marketai.model.MarketTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Lightweight Binance trade stream consumer.
 */
public class BinanceTradeStreamClient {

    private static final Logger LOG = LoggerFactory.getLogger(BinanceTradeStreamClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private volatile WebSocket webSocket;
    private volatile boolean running;

    public synchronized void start(String symbol, Consumer<MarketTrade> listener) {
        if (running) {
            return;
        }
        final String stream = symbol.toLowerCase(Locale.ROOT) + "@trade";
        final URI endpoint = URI.create("wss://stream.binance.com:9443/ws/" + stream);
        running = true;

        webSocket = httpClient.newWebSocketBuilder().buildAsync(endpoint, new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                LOG.info("Connected to Binance trade stream: {}", endpoint);
                WebSocket.Listener.super.onOpen(webSocket);
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                try {
                    final JsonNode node = objectMapper.readTree(data.toString());
                    final String eventSymbol = node.path("s").asText(symbol.toUpperCase(Locale.ROOT));
                    final long eventTime = node.path("T").asLong(System.currentTimeMillis());
                    final double price = node.path("p").asDouble(0.0);
                    final double quantity = node.path("q").asDouble(0.0);
                    if (price > 0.0 && quantity > 0.0) {
                        listener.accept(new MarketTrade(eventSymbol, eventTime, price, quantity));
                    }
                } catch (Exception ex) {
                    LOG.warn("Unable to parse Binance trade payload", ex);
                }
                webSocket.request(1);
                return WebSocket.Listener.super.onText(webSocket, data, last);
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                LOG.info("Binance stream closed ({}): {}", statusCode, reason);
                running = false;
                return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                LOG.error("Binance stream error", error);
                running = false;
                WebSocket.Listener.super.onError(webSocket, error);
            }
        }).join();
    }

    public synchronized void stop() {
        running = false;
        final WebSocket socket = webSocket;
        webSocket = null;
        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }
}
