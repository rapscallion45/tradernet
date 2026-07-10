package com.tradernet.marketai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.marketai.context.MarketContextDataIngestionClient;
import com.tradernet.marketai.context.MarketContextRegistry;
import com.tradernet.marketai.engine.AiSignalEngine;
import com.tradernet.marketai.engine.BarAggregator;
import com.tradernet.marketai.engine.FeatureEngine;
import com.tradernet.marketai.engine.MarketEventPublisher;
import com.tradernet.marketai.forecast.ForecastingClient;
import com.tradernet.marketai.forecast.MarketForecast;
import com.tradernet.marketai.forecast.OllamaNarrativeClient;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.ChartInterval;
import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.marketai.model.MarketContextSnapshot;
import com.tradernet.marketai.model.MarketTrade;
import com.tradernet.marketai.orderbook.BinanceOrderBookClient;
import com.tradernet.marketai.orderbook.OrderBookSnapshot;
import com.tradernet.marketai.stream.BinanceTradeStreamClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.annotation.Resource;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Coordinates market data ingestion, feature generation and signal publishing.
 */
@Singleton
@Startup
public class MarketAiService {

    private static final int DEFAULT_HISTORY_SIZE = 2_000;
    private static final String INSERT_MARKET_BAR_SQL = "INSERT INTO market_bars "
            + "(symbol, bucket, open, high, low, close, volume, source) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final int DEFAULT_SIGNAL_BULL_SCORE_HORIZON_DAYS = 1;
    private static final long DEFAULT_SIGNAL_BULL_SCORE_TTL_MS = Duration.ofMinutes(1).toMillis();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MarketContextRegistry marketContextRegistry = new MarketContextRegistry();
    private final Map<String, BinanceTradeStreamClient> binanceClientsBySymbol = new ConcurrentHashMap<>();
    private final Map<String, BinanceOrderBookClient> orderBookClientsBySymbol = new ConcurrentHashMap<>();
    private final Map<String, BarAggregator> barAggregatorsBySymbol = new ConcurrentHashMap<>();
    private final Map<String, FeatureEngine> featureEnginesBySymbol = new ConcurrentHashMap<>();
    private final Map<String, AiSignalEngine> signalEnginesBySymbol = new ConcurrentHashMap<>();
    private final MarketEventPublisher publisher = new MarketEventPublisher();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final MarketContextDataIngestionClient contextDataIngestionClient = new MarketContextDataIngestionClient(httpClient, OBJECT_MAPPER);
    private final ForecastingClient forecastingClient = new ForecastingClient(httpClient, OBJECT_MAPPER);
    private final OllamaNarrativeClient ollamaNarrativeClient = new OllamaNarrativeClient(httpClient, OBJECT_MAPPER);
    private final Set<String> contextRefreshSymbols = ConcurrentHashMap.newKeySet();
    private final Map<String, Double> signalBullScoresBySymbol = new ConcurrentHashMap<>();
    private final Map<String, Long> signalBullScoreRefreshBySymbol = new ConcurrentHashMap<>();

    @Resource(lookup = "java:/jdbc/TradernetDS")
    private DataSource dataSource;

    private final Deque<MarketBar> bars = new ArrayDeque<>();
    private final Deque<AiSignal> signals = new ArrayDeque<>();

    private volatile List<String> cachedSymbols = List.of("BTCUSDT");
    private volatile long cachedSymbolsAtMs = 0L;

    @PostConstruct
    public void start() {
        final String symbol = System.getProperty("market.ai.symbol", "btcusdt");
        registerContextRefreshSymbols(symbol);
        registerContextRefreshSymbols(System.getProperty("market.ai.context.symbols", symbol));
        ensureLiveSymbol(symbol);
        refreshMarketContexts();
    }

    @PreDestroy
    public void stop() {
        binanceClientsBySymbol.values().forEach(BinanceTradeStreamClient::stop);
        orderBookClientsBySymbol.values().forEach(BinanceOrderBookClient::stop);
        binanceClientsBySymbol.clear();
        orderBookClientsBySymbol.clear();
        barAggregatorsBySymbol.clear();
        featureEnginesBySymbol.clear();
        signalEnginesBySymbol.clear();
    }

    @Lock(LockType.WRITE)
    public void ensureLiveSymbol(String symbol) {
        final String normalizedSymbol = normalizeSymbol(symbol);
        if (normalizedSymbol.isBlank()) {
            return;
        }

        contextRefreshSymbols.add(normalizedSymbol);
        barAggregatorsBySymbol.computeIfAbsent(normalizedSymbol, ignored -> new BarAggregator(1_000L));
        featureEnginesBySymbol.computeIfAbsent(normalizedSymbol, ignored -> new FeatureEngine(marketContextRegistry));
        signalEnginesBySymbol.computeIfAbsent(normalizedSymbol, ignored -> new AiSignalEngine());
        final BinanceTradeStreamClient client = binanceClientsBySymbol.computeIfAbsent(normalizedSymbol, ignored -> new BinanceTradeStreamClient());
        if (!client.isRunning()) {
            client.start(normalizedSymbol.toLowerCase(Locale.ROOT), this::onTrade);
        }
    }

    @Lock(LockType.READ)
    public synchronized List<MarketBar> getBars(int limit) {
        return takeLast(bars, limit);
    }

    @Lock(LockType.READ)
    public List<MarketBar> getBars(String symbol, String intervalToken, int limit) {
        final ChartInterval interval = ChartInterval.parse(intervalToken);
        final List<MarketBar> remoteBars = fetchKlines(symbol, interval, limit);
        if (!remoteBars.isEmpty()) {
            return remoteBars;
        }

        synchronized (this) {
            return takeLast(bars, limit);
        }
    }

    @Lock(LockType.READ)
    public synchronized List<AiSignal> getSignals(int limit) {
        return takeLast(signals, limit);
    }

    @Lock(LockType.READ)
    public List<AiSignal> getSignals(String symbol, int limit) {
        final String normalizedSymbol = normalizeSymbol(symbol);
        final List<AiSignal> matchingSignals;
        synchronized (this) {
            matchingSignals = signals.stream()
                    .filter(signal -> signal.getSymbol() != null)
                    .filter(signal -> signal.getSymbol().trim().toUpperCase(Locale.ROOT).equals(normalizedSymbol))
                    .collect(Collectors.toList());
        }

        if (!matchingSignals.isEmpty()) {
            return takeLast(matchingSignals, limit);
        }

        return generateSignalsFromRemoteBars(normalizedSymbol, limit);
    }

    private List<AiSignal> generateSignalsFromRemoteBars(String normalizedSymbol, int limit) {
        getHydratedMarketContext(normalizedSymbol);
        final List<MarketBar> remoteBars = fetchKlines(normalizedSymbol, ChartInterval.parse("1MIN"), Math.max(50, Math.min(500, limit * 20)));
        if (remoteBars.isEmpty()) {
            return List.of();
        }

        final FeatureEngine remoteFeatureEngine = new FeatureEngine(marketContextRegistry);
        final AiSignalEngine remoteSignalEngine = new AiSignalEngine();
        final List<AiSignal> generatedSignals = new ArrayList<>();
        for (MarketBar bar : remoteBars) {
            final FeatureSnapshot features = enrichWithSignalBullScore(remoteFeatureEngine.onClosedBar(bar));
            final AiSignal signal = remoteSignalEngine.evaluate(features);
            if (signal != null) {
                generatedSignals.add(signal);
            }
        }
        return takeLast(generatedSignals, limit);
    }

    @Lock(LockType.READ)
    public List<String> getSupportedSymbols(String quoteCurrency) {
        final long now = System.currentTimeMillis();
        if (now - cachedSymbolsAtMs > Duration.ofMinutes(15).toMillis()) {
            synchronized (this) {
                if (now - cachedSymbolsAtMs > Duration.ofMinutes(15).toMillis()) {
                    final List<String> remoteSymbols = fetchExchangeSymbols();
                    if (!remoteSymbols.isEmpty()) {
                        cachedSymbols = remoteSymbols;
                    }
                    cachedSymbolsAtMs = now;
                }
            }
        }

        final String normalizedQuoteCurrency = normalizeQuoteCurrency(quoteCurrency);
        final List<String> filtered = cachedSymbols.stream()
                .filter(symbol -> symbol.endsWith(normalizedQuoteCurrency))
                .collect(Collectors.toList());

        if (!filtered.isEmpty()) {
            return filtered;
        }

        final List<String> usdTFallback = cachedSymbols.stream()
                .filter(symbol -> symbol.endsWith("USDT"))
                .collect(Collectors.toList());
        return usdTFallback.isEmpty() ? List.of("BTCUSDT") : usdTFallback;
    }

    @Lock(LockType.READ)
    public MarketContextSnapshot getMarketContext(String symbol) {
        final String normalizedSymbol = normalizeSymbol(symbol);
        contextRefreshSymbols.add(normalizedSymbol);
        final MarketContextSnapshot snapshot = marketContextRegistry.get(normalizedSymbol);
        if (!snapshot.isAvailable()) {
            hydrateMarketContext(normalizedSymbol);
            return marketContextRegistry.get(normalizedSymbol);
        }
        return snapshot;
    }

    @Lock(LockType.READ)
    public MarketForecast getForecast(String symbol, int horizonDays) {
        final String normalizedSymbol = normalizeSymbol(symbol);
        final MarketContextSnapshot snapshot = getHydratedMarketContext(normalizedSymbol);
        final MarketForecast forecast = forecastingClient.forecast(normalizedSymbol, horizonDays, snapshot);
        forecast.setNarrative(ollamaNarrativeClient.summarize(forecast));
        return forecast;
    }

    @Lock(LockType.READ)
    public OrderBookSnapshot getOrderBook(String symbol, int levels) {
        final String normalizedSymbol = normalizeSymbol(symbol);
        final BinanceOrderBookClient client = orderBookClientsBySymbol.computeIfAbsent(
                normalizedSymbol,
                key -> new BinanceOrderBookClient(key, httpClient, OBJECT_MAPPER));
        client.ensureStarted();
        return client.getSnapshot(levels);
    }

    @Lock(LockType.READ)
    public double getBullScore(String symbol, int horizonDays) {
        final String normalizedSymbol = normalizeSymbol(symbol);
        final MarketContextSnapshot snapshot = getHydratedMarketContext(normalizedSymbol);
        return forecastingClient.forecast(normalizedSymbol, horizonDays, snapshot).getBullScore();
    }

    private MarketContextSnapshot getHydratedMarketContext(String normalizedSymbol) {
        contextRefreshSymbols.add(normalizedSymbol);
        MarketContextSnapshot snapshot = marketContextRegistry.get(normalizedSymbol);
        if (!snapshot.isAvailable()) {
            hydrateMarketContext(normalizedSymbol);
            snapshot = marketContextRegistry.get(normalizedSymbol);
        }
        return snapshot;
    }

    @Schedule(hour = "*", minute = "*/15", second = "0", persistent = false)
    @Lock(LockType.READ)
    public void refreshMarketContexts() {
        if (!Boolean.parseBoolean(System.getProperty("market.ai.context.ingestion.enabled", "true"))) {
            return;
        }

        contextRefreshSymbols.addAll(marketContextRegistry.symbols());
        contextRefreshSymbols.forEach(this::hydrateMarketContext);
    }

    @Lock(LockType.WRITE)
    public void updateMarketContext(String symbol, MarketContextSnapshot snapshot) {
        final String normalizedSymbol = normalizeSymbol(symbol);
        contextRefreshSymbols.add(normalizedSymbol);
        marketContextRegistry.update(normalizedSymbol, snapshot);
    }

    @Lock(LockType.READ)
    public AutoCloseable subscribeBars(Consumer<MarketBar> consumer) {
        return publisher.onBar(consumer);
    }

    @Lock(LockType.READ)
    public AutoCloseable subscribeSignals(Consumer<AiSignal> consumer) {
        return publisher.onSignal(consumer);
    }

    private void hydrateMarketContext(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }

        final MarketContextSnapshot current = marketContextRegistry.get(symbol);
        final MarketContextSnapshot hydrated = contextDataIngestionClient.fetch(symbol, current);
        if (hydrated.isAvailable()) {
            marketContextRegistry.update(symbol, hydrated);
        }
    }

    private void registerContextRefreshSymbols(String symbols) {
        if (symbols == null || symbols.isBlank()) {
            return;
        }

        for (String symbol : symbols.split(",")) {
            final String normalizedSymbol = normalizeSymbol(symbol);
            if (!normalizedSymbol.isBlank()) {
                contextRefreshSymbols.add(normalizedSymbol);
            }
        }
    }

    private void onTrade(MarketTrade trade) {
        final String normalizedSymbol = normalizeSymbol(trade.getSymbol());
        final BarAggregator symbolBarAggregator = barAggregatorsBySymbol.computeIfAbsent(normalizedSymbol, ignored -> new BarAggregator(1_000L));
        final FeatureEngine symbolFeatureEngine = featureEnginesBySymbol.computeIfAbsent(normalizedSymbol, ignored -> new FeatureEngine(marketContextRegistry));
        final AiSignalEngine symbolSignalEngine = signalEnginesBySymbol.computeIfAbsent(normalizedSymbol, ignored -> new AiSignalEngine());
        final MarketBar closed = symbolBarAggregator.ingest(trade);
        final MarketBar forming = symbolBarAggregator.snapshotForming();
        if (forming != null) {
            publisher.publishBar(forming);
        }

        if (closed == null) {
            return;
        }

        synchronized (this) {
            appendBounded(bars, closed, DEFAULT_HISTORY_SIZE);
        }
        storeMarketBar(closed);

        final FeatureSnapshot features = enrichWithSignalBullScore(symbolFeatureEngine.onClosedBar(closed));
        final AiSignal signal = symbolSignalEngine.evaluate(features);
        if (signal == null) {
            return;
        }

        synchronized (this) {
            appendBounded(signals, signal, DEFAULT_HISTORY_SIZE);
        }
        publisher.publishSignal(signal);
    }

    private FeatureSnapshot enrichWithSignalBullScore(FeatureSnapshot features) {
        if (features == null || !Boolean.parseBoolean(System.getProperty("market.ai.signalBullScore.enabled", "true"))) {
            return features;
        }

        final String normalizedSymbol = normalizeSymbol(features.getSymbol());
        final long now = System.currentTimeMillis();
        final long ttlMs = Long.parseLong(System.getProperty("market.ai.signalBullScoreTtlMs", String.valueOf(DEFAULT_SIGNAL_BULL_SCORE_TTL_MS)));
        final Long refreshedAt = signalBullScoreRefreshBySymbol.get(normalizedSymbol);
        final Double cachedScore = signalBullScoresBySymbol.get(normalizedSymbol);
        if (cachedScore != null && refreshedAt != null && now - refreshedAt < ttlMs) {
            return features.withForecastBullScore(cachedScore);
        }

        try {
            final int horizonDays = Integer.parseInt(System.getProperty("market.ai.signalBullScoreHorizonDays", String.valueOf(DEFAULT_SIGNAL_BULL_SCORE_HORIZON_DAYS)));
            final double bullScore = getBullScore(normalizedSymbol, horizonDays);
            signalBullScoresBySymbol.put(normalizedSymbol, bullScore);
            signalBullScoreRefreshBySymbol.put(normalizedSymbol, now);
            return features.withForecastBullScore(bullScore);
        } catch (RuntimeException ex) {
            return cachedScore == null ? features : features.withForecastBullScore(cachedScore);
        }
    }

    private void storeMarketBar(MarketBar bar) {
        if (dataSource == null || bar == null) {
            return;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_MARKET_BAR_SQL)) {
            statement.setString(1, normalizeSymbol(bar.getSymbol()));
            statement.setTimestamp(2, new Timestamp(bar.getBucketStart()));
            statement.setDouble(3, bar.getOpen());
            statement.setDouble(4, bar.getHigh());
            statement.setDouble(5, bar.getLow());
            statement.setDouble(6, bar.getClose());
            statement.setDouble(7, bar.getVolume());
            statement.setString(8, "binance-trade-stream");
            statement.executeUpdate();
        } catch (SQLException ex) {
            // Forecasting should degrade gracefully if persistence is unavailable or a duplicate bar arrives.
        }
    }

    private List<String> fetchExchangeSymbols() {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(getBinanceRestBaseUrl() + "/api/v3/exchangeInfo"))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                return List.of();
            }

            final JsonNode payload = OBJECT_MAPPER.readTree(response.body());
            final JsonNode symbolsNode = payload.get("symbols");
            if (symbolsNode == null || !symbolsNode.isArray()) {
                return List.of();
            }

            final List<String> result = new ArrayList<>(symbolsNode.size());
            for (JsonNode node : symbolsNode) {
                if (!"TRADING".equals(node.path("status").asText())) {
                    continue;
                }

                final String symbol = node.path("symbol").asText("").trim().toUpperCase();
                if (!symbol.isEmpty()) {
                    result.add(symbol);
                }
            }

            result.sort(Comparator.naturalOrder());
            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private List<MarketBar> fetchKlines(String symbol, ChartInterval interval, int limit) {
        final String normalizedSymbol = normalizeSymbol(symbol);
        final int boundedLimit = Math.max(1, Math.min(limit, 1_000));
        final String endpoint = getBinanceRestBaseUrl() + "/api/v3/klines?symbol="
                + URLEncoder.encode(normalizedSymbol, StandardCharsets.UTF_8)
                + "&interval=" + URLEncoder.encode(interval.getBinanceInterval(), StandardCharsets.UTF_8)
                + "&limit=" + boundedLimit;

        final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                return List.of();
            }

            final JsonNode payload = OBJECT_MAPPER.readTree(response.body());
            if (!payload.isArray()) {
                return List.of();
            }

            final List<MarketBar> result = new ArrayList<>(payload.size());
            for (JsonNode node : payload) {
                if (!node.isArray() || node.size() < 6) {
                    continue;
                }

                final long bucketStart = node.get(0).asLong();
                final double open = node.get(1).asDouble();
                final double high = node.get(2).asDouble();
                final double low = node.get(3).asDouble();
                final double close = node.get(4).asDouble();
                final double volume = node.get(5).asDouble();
                result.add(new MarketBar(normalizedSymbol, bucketStart, open, high, low, close, volume, true));
            }

            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (IOException ex) {
            return List.of();
        }
    }


    private String normalizeQuoteCurrency(String rawCurrency) {
        if (rawCurrency == null || rawCurrency.isBlank()) {
            return "USDT";
        }

        final String upper = rawCurrency.trim().toUpperCase();
        if ("USD".equals(upper)) {
            return "USDT";
        }

        return upper;
    }

    private String getBinanceRestBaseUrl() {
        final String rawUrl = System.getProperty("market.ai.binance.restBaseUrl", "https://api.binance.com");
        final String trimmed = rawUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String normalizeSymbol(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            return "BTCUSDT";
        }

        final String upper = rawSymbol.trim().toUpperCase();
        if ("BTCUSD".equals(upper)) {
            return "BTCUSDT";
        }
        if ("ETHUSD".equals(upper)) {
            return "ETHUSDT";
        }
        return upper;
    }

    private <T> void appendBounded(Deque<T> deque, T value, int maxSize) {
        deque.addLast(value);
        while (deque.size() > maxSize) {
            deque.removeFirst();
        }
    }

    private <T> List<T> takeLast(Deque<T> deque, int limit) {
        return takeLast(new ArrayList<>(deque), limit);
    }

    private <T> List<T> takeLast(List<T> values, int limit) {
        final int size = values.size();
        final int effectiveLimit = Math.max(1, limit);
        final int skip = Math.max(0, size - effectiveLimit);
        final List<T> snapshot = new ArrayList<>(size);
        int index = 0;
        for (T item : values) {
            if (index++ >= skip) {
                snapshot.add(item);
            }
        }
        return snapshot;
    }
}
