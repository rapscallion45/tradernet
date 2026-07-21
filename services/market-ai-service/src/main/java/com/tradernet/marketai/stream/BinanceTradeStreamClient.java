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
    private static final int MAX_TEXT_MESSAGE_CHARS = 256_000;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String webSocketBaseUrl;

    private volatile WebSocket webSocket;
    private volatile boolean running;

    public BinanceTradeStreamClient(String webSocketBaseUrl) {
        this.webSocketBaseUrl = webSocketBaseUrl;
    }

    public synchronized void start(String symbol, Consumer<MarketTrade> listener) {
        if (running) {
            return;
        }
        final String stream = symbol.toLowerCase(Locale.ROOT) + "@trade";
        final URI endpoint = URI.create(webSocketBaseUrl + "/" + stream);
        running = true;

        try {
            webSocket = httpClient.newWebSocketBuilder().buildAsync(endpoint, new WebSocket.Listener() {
                private final WebSocketTextMessageBuffer textMessages = new WebSocketTextMessageBuffer(MAX_TEXT_MESSAGE_CHARS);

                @Override
                public void onOpen(WebSocket webSocket) {
                    LOG.info("Connected to Binance trade stream: {}", endpoint);
                    webSocket.request(1);
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    try {
                        final String payload = textMessages.append(data, last);
                        if (payload != null) {
                            final JsonNode node = objectMapper.readTree(payload);
                            final String eventSymbol = node.path("s").asText(symbol.toUpperCase(Locale.ROOT));
                            final long eventTime = node.path("T").asLong(System.currentTimeMillis());
                            final double price = node.path("p").asDouble(0.0);
                            final double quantity = node.path("q").asDouble(0.0);
                            if (price > 0.0 && quantity > 0.0) {
                                listener.accept(new MarketTrade(eventSymbol, eventTime, price, quantity));
                            }
                        }
                    } catch (Exception ex) {
                        textMessages.reset();
                        LOG.warn("Unable to parse Binance trade payload", ex);
                    }
                    webSocket.request(1);
                    return null;
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    textMessages.reset();
                    LOG.info("Binance stream closed ({}): {}", statusCode, reason);
                    running = false;
                    return null;
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    textMessages.reset();
                    LOG.error("Binance stream error", error);
                    running = false;
                }
            }).join();
        } catch (RuntimeException ex) {
            running = false;
            webSocket = null;
            LOG.warn("Unable to connect to Binance trade stream: {}", endpoint, ex);
        }
    }

    public boolean isRunning() {
        return running;
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
