package com.tradernet.marketai.orderbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.stream.WebSocketTextMessageBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import com.tradernet.marketai.orderbook.BinanceOrderBookPayloadParser.DepthUpdate;
import com.tradernet.marketai.orderbook.BinanceOrderBookPayloadParser.LevelUpdate;
import com.tradernet.marketai.orderbook.BinanceOrderBookPayloadParser.SnapshotData;

/**
 * Maintains a Binance aggregated L2 order book using the documented snapshot + diff-depth flow.
 */
public class BinanceOrderBookClient {

    private static final Logger LOG = LoggerFactory.getLogger(BinanceOrderBookClient.class);
    private static final int MAX_BUFFERED_UPDATES = 1_000;
    private static final long START_RETRY_COOLDOWN_MS = Duration.ofSeconds(10).toMillis();
    private static final long RESYNC_RETRY_COOLDOWN_MS = Duration.ofSeconds(5).toMillis();
    private static final int MAX_TEXT_MESSAGE_CHARS = 1_000_000;

    private final String symbol;
    private final String restBaseUrl;
    private final String wsBaseUrl;
    private final HttpClient httpClient;
    private final BinanceOrderBookPayloadParser payloadParser;
    private final OrderBookSnapshotFactory snapshotFactory = new OrderBookSnapshotFactory();
    private final Consumer<String> resyncRequester;
    private final int exchangeSnapshotLimit;
    private final long staleAfterMs;
    private final NavigableMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<BigDecimal, BigDecimal> asks = new TreeMap<>();
    private final ArrayDeque<DepthUpdate> bufferedUpdates = new ArrayDeque<>();

    private WebSocket webSocket;
    private boolean running;
    private boolean streamSynchronized;
    private long lastUpdateId = -1L;
    private long lastExchangeEventTimeMs;
    private long lastAppliedAtMs;
    private long lastStartAttemptAtMs;
    private long lastSyncAttemptAtMs;
    private long resyncCount;
    private String lastError;

    public BinanceOrderBookClient(
        String symbol,
        HttpClient httpClient,
        ObjectMapper objectMapper,
        Consumer<String> resyncRequester,
        String restBaseUrl,
        String wsBaseUrl,
        int exchangeSnapshotLimit,
        long staleAfterMs
    ) {
        this.symbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        this.httpClient = httpClient;
        this.payloadParser = new BinanceOrderBookPayloadParser(objectMapper);
        this.resyncRequester = resyncRequester;
        this.restBaseUrl = normalizeBaseUrl(restBaseUrl);
        this.wsBaseUrl = normalizeBaseUrl(wsBaseUrl);
        this.exchangeSnapshotLimit = normalizeExchangeSnapshotLimit(exchangeSnapshotLimit);
        this.staleAfterMs = Math.max(1_000L, staleAfterMs);
    }

    public void ensureStarted() {
        try {
            final URI endpoint = URI.create(wsBaseUrl + "/" + symbol.toLowerCase(Locale.ROOT) + "@depth@100ms");
            synchronized (this) {
                if (running) {
                    return;
                }

                final long now = System.currentTimeMillis();
                if (now - lastStartAttemptAtMs < START_RETRY_COOLDOWN_MS) {
                    return;
                }
                lastStartAttemptAtMs = now;
                running = true;
                streamSynchronized = false;
                bufferedUpdates.clear();
            }

            final WebSocket socket = httpClient.newWebSocketBuilder().buildAsync(endpoint, new WebSocket.Listener() {
                private final WebSocketTextMessageBuffer textMessages = new WebSocketTextMessageBuffer(MAX_TEXT_MESSAGE_CHARS);

                @Override
                public void onOpen(WebSocket webSocket) {
                    LOG.info("Connected to Binance order book stream: {}", endpoint);
                    webSocket.request(1);
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    try {
                        final String payload = textMessages.append(data, last);
                        if (payload != null) {
                            handleUpdate(payloadParser.parseDepthUpdate(payload));
                        }
                    } catch (RuntimeException ex) {
                        textMessages.reset();
                        markError("Unable to parse Binance order book payload", ex);
                    }
                    webSocket.request(1);
                    return null;
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    textMessages.reset();
                    synchronized (BinanceOrderBookClient.this) {
                        running = false;
                        streamSynchronized = false;
                        if (!"shutdown".equals(reason)) {
                            lastError = "Binance order book stream closed: " + statusCode + " " + reason;
                        }
                    }
                    LOG.info("Binance order book stream closed ({}): {}", statusCode, reason);
                    return null;
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    textMessages.reset();
                    synchronized (BinanceOrderBookClient.this) {
                        running = false;
                        streamSynchronized = false;
                    }
                    markError("Binance order book stream error", error);
                }
            }).join();

            synchronized (this) {
                if (!running) {
                    socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
                    return;
                }
                webSocket = socket;
            }
            resync("initial snapshot");
        } catch (CompletionException | IllegalArgumentException ex) {
            synchronized (this) {
                running = false;
                streamSynchronized = false;
                webSocket = null;
            }
            markError("Unable to connect to Binance order book stream", ex);
            refreshSnapshotOnly();
        }
    }

    public OrderBookSnapshot getSnapshot(int requestedLevels) {
        synchronized (this) {
            return snapshotFactory.create(
                symbol,
                bids,
                asks,
                requestedLevels,
                running,
                streamSynchronized,
                lastUpdateId,
                lastExchangeEventTimeMs,
                lastAppliedAtMs,
                staleAfterMs,
                resyncCount,
                exchangeSnapshotLimit,
                lastError
            );
        }
    }

    public synchronized boolean shouldRetrySync() {
        return running
                && !streamSynchronized
                && System.currentTimeMillis() - lastSyncAttemptAtMs > RESYNC_RETRY_COOLDOWN_MS;
    }

    public void retrySync() {
        resync("retry snapshot");
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void stop() {
        running = false;
        streamSynchronized = false;
        final WebSocket socket = webSocket;
        webSocket = null;
        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }

    private void handleUpdate(DepthUpdate update) {
        final String resyncReason;
        synchronized (this) {
            if (update == null) {
                return;
            }

            if (!streamSynchronized) {
                bufferedUpdates.addLast(update);
                while (bufferedUpdates.size() > MAX_BUFFERED_UPDATES) {
                    bufferedUpdates.removeFirst();
                }
                return;
            }

            if (update.finalUpdateId <= lastUpdateId) {
                return;
            }

            if (update.previousFinalUpdateId > 0L && update.previousFinalUpdateId != lastUpdateId) {
                resyncReason = "previous update id " + update.previousFinalUpdateId + " did not match " + lastUpdateId;
            } else if (update.firstUpdateId > lastUpdateId + 1L) {
                resyncReason = "first update id " + update.firstUpdateId + " skipped local update id " + lastUpdateId;
            } else {
                applyUpdate(update);
                lastError = null;
                return;
            }
        }

        requestResyncAfterGap(resyncReason);
    }

    private void requestResyncAfterGap(String reason) {
        synchronized (this) {
            lastError = "Missed Binance depth update; resyncing from REST snapshot (" + reason + ")";
            resyncCount++;
            streamSynchronized = false;
            bufferedUpdates.clear();
        }
        resyncRequester.accept("gap");
    }

    private void resync(String reason) {
        synchronized (this) {
            lastSyncAttemptAtMs = System.currentTimeMillis();
            streamSynchronized = false;
        }

        final SnapshotData snapshot = fetchSnapshot();
        if (snapshot == null) {
            return;
        }

        synchronized (this) {
            bids.clear();
            bids.putAll(snapshot.bids);
            asks.clear();
            asks.putAll(snapshot.asks);
            lastUpdateId = snapshot.lastUpdateId;
            lastExchangeEventTimeMs = snapshot.receivedAtMs;
            lastAppliedAtMs = snapshot.receivedAtMs;
            streamSynchronized = true;
            lastError = null;

            while (!bufferedUpdates.isEmpty()) {
                final DepthUpdate buffered = bufferedUpdates.removeFirst();
                if (buffered.finalUpdateId <= lastUpdateId) {
                    continue;
                }
                if (buffered.firstUpdateId > lastUpdateId + 1L) {
                    streamSynchronized = false;
                    lastError = "Buffered Binance depth updates had a gap after " + reason;
                    resyncCount++;
                    return;
                }
                applyUpdate(buffered);
            }
        }
    }

    private void refreshSnapshotOnly() {
        final SnapshotData snapshot = fetchSnapshot();
        if (snapshot == null) {
            return;
        }

        synchronized (this) {
            bids.clear();
            bids.putAll(snapshot.bids);
            asks.clear();
            asks.putAll(snapshot.asks);
            lastUpdateId = snapshot.lastUpdateId;
            lastExchangeEventTimeMs = snapshot.receivedAtMs;
            lastAppliedAtMs = snapshot.receivedAtMs;
            streamSynchronized = false;
        }
    }

    private SnapshotData fetchSnapshot() {
        final String endpoint = restBaseUrl + "/api/v3/depth?symbol="
                + URLEncoder.encode(symbol, StandardCharsets.UTF_8)
                + "&limit=" + exchangeSnapshotLimit;
        final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                setLastError("Binance order book snapshot returned HTTP " + response.statusCode());
                return null;
            }

            final SnapshotData snapshot = payloadParser.parseSnapshot(response.body(), System.currentTimeMillis());
            if (snapshot == null) {
                setLastError("Binance order book snapshot did not include lastUpdateId");
                return null;
            }
            return snapshot;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            setLastError("Interrupted while fetching Binance order book snapshot");
            return null;
        } catch (IOException | RuntimeException ex) {
            markError("Unable to fetch Binance order book snapshot", ex);
            return null;
        }
    }

    private void applyUpdate(DepthUpdate update) {
        applySideUpdate(bids, update.bids);
        applySideUpdate(asks, update.asks);
        lastUpdateId = update.finalUpdateId;
        lastExchangeEventTimeMs = update.eventTime;
        lastAppliedAtMs = System.currentTimeMillis();
    }

    private void applySideUpdate(NavigableMap<BigDecimal, BigDecimal> side, List<LevelUpdate> updates) {
        for (LevelUpdate update : updates) {
            if (update.quantity.signum() == 0) {
                side.remove(update.price);
            } else {
                side.put(update.price, update.quantity);
            }
        }
    }

    private synchronized void markError(String message, Throwable error) {
        lastError = message + ": " + error.getMessage();
        LOG.warn(message, error);
    }

    private synchronized void setLastError(String message) {
        lastError = message;
    }

    private static int normalizeExchangeSnapshotLimit(int requestedLimit) {
        if (requestedLimit <= 5) {
            return 5;
        }
        if (requestedLimit <= 10) {
            return 10;
        }
        if (requestedLimit <= 20) {
            return 20;
        }
        if (requestedLimit <= 50) {
            return 50;
        }
        if (requestedLimit <= 100) {
            return 100;
        }
        if (requestedLimit <= 500) {
            return 500;
        }
        if (requestedLimit <= 1_000) {
            return 1_000;
        }
        return 5_000;
    }

    private static String normalizeBaseUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }

        final String trimmed = rawUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

}
