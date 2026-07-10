package com.tradernet.marketai.orderbook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Maintains a Binance aggregated L2 order book using the documented snapshot + diff-depth flow.
 */
public class BinanceOrderBookClient {

    private static final Logger LOG = LoggerFactory.getLogger(BinanceOrderBookClient.class);
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int DEFAULT_EXCHANGE_SNAPSHOT_LIMIT = 5_000;
    private static final int MAX_BUFFERED_UPDATES = 1_000;
    private static final long START_RETRY_COOLDOWN_MS = Duration.ofSeconds(10).toMillis();
    private static final long RESYNC_RETRY_COOLDOWN_MS = Duration.ofSeconds(5).toMillis();
    private static final long DEFAULT_STALE_AFTER_MS = Duration.ofSeconds(30).toMillis();

    private final String symbol;
    private final String restBaseUrl;
    private final String wsBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
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

    public BinanceOrderBookClient(String symbol, HttpClient httpClient, ObjectMapper objectMapper) {
        this.symbol = normalizeSymbol(symbol);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.restBaseUrl = normalizeBaseUrl(System.getProperty("market.ai.binance.restBaseUrl", "https://api.binance.com"));
        this.wsBaseUrl = normalizeBaseUrl(System.getProperty("market.ai.binance.wsBaseUrl", "wss://stream.binance.com:9443/ws"));
        this.exchangeSnapshotLimit = normalizeExchangeSnapshotLimit(systemInt("market.ai.orderBook.snapshotLimit", DEFAULT_EXCHANGE_SNAPSHOT_LIMIT));
        this.staleAfterMs = systemLong("market.ai.orderBook.staleAfterMs", DEFAULT_STALE_AFTER_MS);
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
            }

            final WebSocket socket = httpClient.newWebSocketBuilder().buildAsync(endpoint, new WebSocket.Listener() {
                @Override
                public void onOpen(WebSocket webSocket) {
                    LOG.info("Connected to Binance order book stream: {}", endpoint);
                    WebSocket.Listener.super.onOpen(webSocket);
                    webSocket.request(1);
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    try {
                        handleUpdate(parseDepthUpdate(data.toString()));
                    } catch (RuntimeException ex) {
                        markError("Unable to parse Binance order book payload", ex);
                    }
                    webSocket.request(1);
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    synchronized (BinanceOrderBookClient.this) {
                        running = false;
                        streamSynchronized = false;
                        if (!"shutdown".equals(reason)) {
                            lastError = "Binance order book stream closed: " + statusCode + " " + reason;
                        }
                    }
                    LOG.info("Binance order book stream closed ({}): {}", statusCode, reason);
                    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    synchronized (BinanceOrderBookClient.this) {
                        running = false;
                        streamSynchronized = false;
                    }
                    markError("Binance order book stream error", error);
                    WebSocket.Listener.super.onError(webSocket, error);
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
        final boolean shouldRetrySync;
        synchronized (this) {
            shouldRetrySync = running
                    && !streamSynchronized
                    && System.currentTimeMillis() - lastSyncAttemptAtMs > RESYNC_RETRY_COOLDOWN_MS;
        }
        if (shouldRetrySync) {
            resync("retry snapshot");
        }

        synchronized (this) {
            final int levels = boundRequestedLevels(requestedLevels);
            final List<OrderBookLevel> bidLevels = buildLevels(bids, levels);
            final List<OrderBookLevel> askLevels = buildLevels(asks, levels);
            final long now = System.currentTimeMillis();
            final boolean hasData = !bidLevels.isEmpty() || !askLevels.isEmpty();
            final boolean stale = hasData && lastAppliedAtMs > 0 && now - lastAppliedAtMs > staleAfterMs;
            final OrderBookStatus status = resolveStatus(hasData, stale);
            final double bestBid = bidLevels.isEmpty() ? 0.0 : bidLevels.get(0).getPrice();
            final double bestAsk = askLevels.isEmpty() ? 0.0 : askLevels.get(0).getPrice();
            final double midPrice = bestBid > 0.0 && bestAsk > 0.0 ? (bestBid + bestAsk) / 2.0 : 0.0;
            final double spread = bestBid > 0.0 && bestAsk > 0.0 ? Math.max(0.0, bestAsk - bestBid) : 0.0;
            final double spreadPercent = midPrice > 0.0 ? (spread / midPrice) * 100.0 : 0.0;
            final double bidDepthNotional = sumNotional(bidLevels);
            final double askDepthNotional = sumNotional(askLevels);
            final double totalDepthNotional = bidDepthNotional + askDepthNotional;
            final double depthImbalancePercent = totalDepthNotional > 0.0
                    ? ((bidDepthNotional - askDepthNotional) / totalDepthNotional) * 100.0
                    : 0.0;
            final long updateLatencyMs = lastExchangeEventTimeMs > 0L ? Math.max(0L, now - lastExchangeEventTimeMs) : 0L;

            return new OrderBookSnapshot(
                    symbol,
                    inferQuoteCurrency(symbol),
                    status,
                    "binance-spot",
                    "AGGREGATED_L2",
                    statusMessage(status),
                    lastExchangeEventTimeMs,
                    lastUpdateId,
                    updateLatencyMs,
                    resyncCount,
                    exchangeSnapshotLimit,
                    levels,
                    stale,
                    bestBid,
                    bestAsk,
                    midPrice,
                    spread,
                    spreadPercent,
                    bidDepthNotional,
                    askDepthNotional,
                    depthImbalancePercent,
                    bidLevels,
                    askLevels);
        }
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

        resyncAfterGap(resyncReason);
    }

    private void resyncAfterGap(String reason) {
        synchronized (this) {
            lastError = "Missed Binance depth update; resyncing from REST snapshot (" + reason + ")";
            resyncCount++;
        }
        resync("gap");
    }

    private void resync(String reason) {
        synchronized (this) {
            lastSyncAttemptAtMs = System.currentTimeMillis();
            streamSynchronized = false;
            bufferedUpdates.clear();
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

            final JsonNode payload = objectMapper.readTree(response.body());
            final long snapshotUpdateId = payload.path("lastUpdateId").asLong(-1L);
            if (snapshotUpdateId < 0L) {
                setLastError("Binance order book snapshot did not include lastUpdateId");
                return null;
            }

            return new SnapshotData(
                    snapshotUpdateId,
                    parseLevels(payload.path("bids")),
                    parseLevels(payload.path("asks")),
                    System.currentTimeMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            setLastError("Interrupted while fetching Binance order book snapshot");
            return null;
        } catch (IOException | RuntimeException ex) {
            markError("Unable to fetch Binance order book snapshot", ex);
            return null;
        }
    }

    private DepthUpdate parseDepthUpdate(String payload) {
        try {
            final JsonNode node = objectMapper.readTree(payload);
            final long firstUpdateId = node.path("U").asLong(-1L);
            final long finalUpdateId = node.path("u").asLong(-1L);
            if (firstUpdateId < 0L || finalUpdateId < 0L) {
                return null;
            }

            return new DepthUpdate(
                    firstUpdateId,
                    finalUpdateId,
                    node.path("pu").asLong(-1L),
                    node.path("E").asLong(System.currentTimeMillis()),
                    parseLevelUpdates(node.path("b")),
                    parseLevelUpdates(node.path("a")));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid Binance depth update payload", ex);
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

    private NavigableMap<BigDecimal, BigDecimal> parseLevels(JsonNode levelsNode) {
        final NavigableMap<BigDecimal, BigDecimal> levels = new TreeMap<>();
        if (levelsNode == null || !levelsNode.isArray()) {
            return levels;
        }

        for (JsonNode levelNode : levelsNode) {
            final LevelUpdate level = parseLevel(levelNode);
            if (level != null && level.quantity.signum() > 0) {
                levels.put(level.price, level.quantity);
            }
        }
        return levels;
    }

    private List<LevelUpdate> parseLevelUpdates(JsonNode levelsNode) {
        final List<LevelUpdate> updates = new ArrayList<>();
        if (levelsNode == null || !levelsNode.isArray()) {
            return updates;
        }

        for (JsonNode levelNode : levelsNode) {
            final LevelUpdate level = parseLevel(levelNode);
            if (level != null) {
                updates.add(level);
            }
        }
        return updates;
    }

    private LevelUpdate parseLevel(JsonNode levelNode) {
        if (levelNode == null || !levelNode.isArray() || levelNode.size() < 2) {
            return null;
        }

        final BigDecimal price = decimalValue(levelNode.get(0));
        final BigDecimal quantity = decimalValue(levelNode.get(1));
        if (price == null || quantity == null || price.signum() <= 0 || quantity.signum() < 0) {
            return null;
        }

        return new LevelUpdate(price, quantity);
    }

    private BigDecimal decimalValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<OrderBookLevel> buildLevels(NavigableMap<BigDecimal, BigDecimal> levels, int limit) {
        final List<LevelDraft> drafts = new ArrayList<>();
        BigDecimal cumulativeQuantity = BigDecimal.ZERO;
        BigDecimal cumulativeNotional = BigDecimal.ZERO;
        int count = 0;

        for (Map.Entry<BigDecimal, BigDecimal> entry : levels.entrySet()) {
            if (count++ >= limit) {
                break;
            }

            final BigDecimal price = entry.getKey();
            final BigDecimal quantity = entry.getValue();
            final BigDecimal notional = price.multiply(quantity, MC);
            cumulativeQuantity = cumulativeQuantity.add(quantity, MC);
            cumulativeNotional = cumulativeNotional.add(notional, MC);
            drafts.add(new LevelDraft(price, quantity, notional, cumulativeQuantity, cumulativeNotional));
        }

        if (drafts.isEmpty()) {
            return List.of();
        }

        final BigDecimal totalNotional = drafts.get(drafts.size() - 1).cumulativeNotional;
        final List<OrderBookLevel> result = new ArrayList<>(drafts.size());
        for (LevelDraft draft : drafts) {
            final double depthPercent = totalNotional.signum() > 0
                    ? draft.cumulativeNotional.multiply(ONE_HUNDRED, MC).divide(totalNotional, MC).doubleValue()
                    : 0.0;
            result.add(new OrderBookLevel(
                    draft.price.doubleValue(),
                    draft.quantity.doubleValue(),
                    draft.notional.doubleValue(),
                    draft.cumulativeQuantity.doubleValue(),
                    draft.cumulativeNotional.doubleValue(),
                    depthPercent));
        }
        return result;
    }

    private double sumNotional(List<OrderBookLevel> levels) {
        double total = 0.0;
        for (OrderBookLevel level : levels) {
            total += level.getNotional();
        }
        return total;
    }

    private OrderBookStatus resolveStatus(boolean hasData, boolean stale) {
        if (streamSynchronized && running && !stale) {
            return OrderBookStatus.LIVE;
        }
        if (streamSynchronized && running) {
            return OrderBookStatus.STALE;
        }
        if (hasData && !running) {
            return OrderBookStatus.SNAPSHOT_ONLY;
        }
        if (hasData) {
            return OrderBookStatus.SYNCING;
        }
        return lastError == null ? OrderBookStatus.SYNCING : OrderBookStatus.UNAVAILABLE;
    }

    private String statusMessage(OrderBookStatus status) {
        switch (status) {
            case LIVE:
                return "Live Binance aggregated L2 depth stream";
            case STALE:
                return "Order book stream has not applied an update recently";
            case SNAPSHOT_ONLY:
                return lastError == null ? "REST snapshot available while live stream reconnects" : "REST snapshot only: " + lastError;
            case UNAVAILABLE:
                return lastError == null ? "Order book unavailable" : lastError;
            case SYNCING:
            default:
                return lastError == null ? "Syncing Binance depth stream with REST snapshot" : lastError;
        }
    }

    private synchronized void markError(String message, Throwable error) {
        lastError = message + ": " + error.getMessage();
        LOG.warn(message, error);
    }

    private synchronized void setLastError(String message) {
        lastError = message;
    }

    private static int boundRequestedLevels(int requestedLevels) {
        return Math.max(5, Math.min(50, requestedLevels));
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

    private static int systemInt(String propertyName, int fallback) {
        try {
            return Integer.parseInt(System.getProperty(propertyName, String.valueOf(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static long systemLong(String propertyName, long fallback) {
        try {
            return Long.parseLong(System.getProperty(propertyName, String.valueOf(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String normalizeBaseUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }

        final String trimmed = rawUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String normalizeSymbol(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            return "BTCUSDT";
        }
        return rawSymbol.trim().toUpperCase(Locale.ROOT);
    }

    private static String inferQuoteCurrency(String symbol) {
        if (symbol.endsWith("USDT") || symbol.endsWith("USD")) {
            return "USD";
        }
        if (symbol.endsWith("EUR")) {
            return "EUR";
        }
        if (symbol.endsWith("GBP")) {
            return "GBP";
        }
        return "USD";
    }

    private static class DepthUpdate {
        private final long firstUpdateId;
        private final long finalUpdateId;
        private final long previousFinalUpdateId;
        private final long eventTime;
        private final List<LevelUpdate> bids;
        private final List<LevelUpdate> asks;

        private DepthUpdate(long firstUpdateId, long finalUpdateId, long previousFinalUpdateId, long eventTime, List<LevelUpdate> bids, List<LevelUpdate> asks) {
            this.firstUpdateId = firstUpdateId;
            this.finalUpdateId = finalUpdateId;
            this.previousFinalUpdateId = previousFinalUpdateId;
            this.eventTime = eventTime;
            this.bids = bids;
            this.asks = asks;
        }
    }

    private static class LevelUpdate {
        private final BigDecimal price;
        private final BigDecimal quantity;

        private LevelUpdate(BigDecimal price, BigDecimal quantity) {
            this.price = price;
            this.quantity = quantity;
        }
    }

    private static class SnapshotData {
        private final long lastUpdateId;
        private final NavigableMap<BigDecimal, BigDecimal> bids;
        private final NavigableMap<BigDecimal, BigDecimal> asks;
        private final long receivedAtMs;

        private SnapshotData(long lastUpdateId, NavigableMap<BigDecimal, BigDecimal> bids, NavigableMap<BigDecimal, BigDecimal> asks, long receivedAtMs) {
            this.lastUpdateId = lastUpdateId;
            this.bids = new TreeMap<>(Comparator.reverseOrder());
            this.bids.putAll(bids);
            this.asks = new TreeMap<>();
            this.asks.putAll(asks);
            this.receivedAtMs = receivedAtMs;
        }
    }

    private static class LevelDraft {
        private final BigDecimal price;
        private final BigDecimal quantity;
        private final BigDecimal notional;
        private final BigDecimal cumulativeQuantity;
        private final BigDecimal cumulativeNotional;

        private LevelDraft(BigDecimal price, BigDecimal quantity, BigDecimal notional, BigDecimal cumulativeQuantity, BigDecimal cumulativeNotional) {
            this.price = price;
            this.quantity = quantity;
            this.notional = notional;
            this.cumulativeQuantity = cumulativeQuantity;
            this.cumulativeNotional = cumulativeNotional;
        }
    }
}
