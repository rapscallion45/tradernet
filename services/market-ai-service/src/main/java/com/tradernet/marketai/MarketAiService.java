package com.tradernet.marketai;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.context.MarketContextService;
import com.tradernet.marketai.engine.AiSignalEngine;
import com.tradernet.marketai.engine.BarAggregator;
import com.tradernet.marketai.engine.FeatureEngine;
import com.tradernet.marketai.engine.MarketEventPublisher;
import com.tradernet.marketai.forecast.MarketForecast;
import com.tradernet.marketai.forecast.MarketForecastService;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.ChartInterval;
import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.marketai.model.MarketContextSnapshot;
import com.tradernet.marketai.model.MarketContextUpdateRequest;
import com.tradernet.marketai.model.MarketTrade;
import com.tradernet.marketai.orderbook.MarketOrderBookService;
import com.tradernet.marketai.orderbook.OrderBookSnapshot;
import com.tradernet.marketai.stream.BinanceTradeStreamClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Schedule;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Public market AI facade used by API resources and websocket endpoints.
 */
@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketAiService {

    @EJB
    private MarketContextService marketContexts;

    @EJB
    private BinanceMarketDataClient marketDataClient;

    @EJB
    private MarketOrderBookService orderBooks;

    @EJB
    private MarketForecastService marketForecastService;

    @EJB
    private MarketAiConfiguration configuration;

    @EJB
    private MarketHistoryBuffer history;

    @EJB
    private MarketBarStorageService barStorageService;

    @EJB
    private MarketEventPublisher publisher;

    @Resource
    private SessionContext sessionContext;

    private final Map<String, BinanceTradeStreamClient> binanceClientsBySymbol = new ConcurrentHashMap<>();
    private final Map<String, BarAggregator> barAggregatorsBySymbol = new ConcurrentHashMap<>();
    private final Map<String, FeatureEngine> featureEnginesBySymbol = new ConcurrentHashMap<>();
    private final Map<String, AiSignalEngine> signalEnginesBySymbol = new ConcurrentHashMap<>();
    private final Set<String> tradeStartsInFlight = ConcurrentHashMap.newKeySet();

    private volatile List<String> cachedSymbols = List.of("BTCUSDT");
    private volatile long cachedSymbolsAtMs = 0L;

    @PostConstruct
    public void start() {
        final String symbol = configuration.getDefaultSymbol();
        marketContexts.registerSymbols(symbol);
        marketContexts.registerSymbols(configuration.getContextSymbols());
        ensureLiveSymbol(symbol);
        refreshMarketContexts();
    }

    @PreDestroy
    public void stop() {
        binanceClientsBySymbol.values().forEach(BinanceTradeStreamClient::stop);
        orderBooks.stop();
        binanceClientsBySymbol.clear();
        barAggregatorsBySymbol.clear();
        featureEnginesBySymbol.clear();
        signalEnginesBySymbol.clear();
        tradeStartsInFlight.clear();
    }

    @Lock(LockType.READ)
    public void ensureLiveSymbol(String symbol) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        if (normalizedSymbol.isBlank()) {
            return;
        }

        marketContexts.registerSymbol(normalizedSymbol);
        barAggregatorsBySymbol.computeIfAbsent(normalizedSymbol, ignored -> new BarAggregator(1_000L));
        featureEnginesBySymbol.computeIfAbsent(normalizedSymbol, ignored -> new FeatureEngine(marketContexts.registry()));
        signalEnginesBySymbol.computeIfAbsent(
            normalizedSymbol,
            ignored -> new AiSignalEngine(configuration.getScoringSettings())
        );
        final BinanceTradeStreamClient client = binanceClientsBySymbol.computeIfAbsent(
            normalizedSymbol,
            ignored -> new BinanceTradeStreamClient(configuration.getBinanceWebSocketBaseUrl())
        );
        if (!client.isRunning()) {
            requestTradeStart(normalizedSymbol);
        }
    }

    @Asynchronous
    @Lock(LockType.READ)
    public void startLiveSymbol(String normalizedSymbol) {
        try {
            final BinanceTradeStreamClient client = binanceClientsBySymbol.computeIfAbsent(
                normalizedSymbol,
                ignored -> new BinanceTradeStreamClient(configuration.getBinanceWebSocketBaseUrl())
            );
            if (!client.isRunning()) {
                final MarketAiService tradeHandler = sessionContext.getBusinessObject(MarketAiService.class);
                client.start(normalizedSymbol.toLowerCase(Locale.ROOT), tradeHandler::onTrade);
            }
        } finally {
            tradeStartsInFlight.remove(normalizedSymbol);
        }
    }

    @Lock(LockType.READ)
    public List<MarketBar> getBars(String symbol, String intervalToken, int limit) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final ChartInterval interval = ChartInterval.parse(intervalToken);
        final List<MarketBar> remoteBars = marketDataClient.fetchKlines(normalizedSymbol, interval, limit);
        if (!remoteBars.isEmpty()) {
            return remoteBars;
        }

        return history.getBarsForSymbol(normalizedSymbol, limit);
    }

    @Lock(LockType.READ)
    public List<AiSignal> getSignals(String symbol, int limit) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final List<AiSignal> matchingSignals = history.getSignalsForSymbol(normalizedSymbol, limit);
        if (!matchingSignals.isEmpty()) {
            return matchingSignals;
        }

        return generateSignalsFromRemoteBars(normalizedSymbol, limit);
    }

    @Lock(LockType.READ)
    public List<String> getSupportedSymbols(String quoteCurrency) {
        final long now = System.currentTimeMillis();
        if (now - cachedSymbolsAtMs > Duration.ofMinutes(15).toMillis()) {
            synchronized (this) {
                if (now - cachedSymbolsAtMs > Duration.ofMinutes(15).toMillis()) {
                    final List<String> remoteSymbols = marketDataClient.fetchExchangeSymbols();
                    if (!remoteSymbols.isEmpty()) {
                        cachedSymbols = remoteSymbols;
                    }
                    cachedSymbolsAtMs = now;
                }
            }
        }

        final String normalizedQuoteCurrency = MarketSymbolNormalizer.normalizeQuoteCurrency(quoteCurrency);
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
        return marketContexts.get(symbol);
    }

    @Lock(LockType.READ)
    public MarketForecast getForecast(String symbol, int horizonDays) {
        return marketForecastService.getForecast(symbol, horizonDays);
    }

    @Lock(LockType.READ)
    public OrderBookSnapshot getOrderBook(String symbol, int levels) {
        return orderBooks.getOrderBook(symbol, levels);
    }

    @Lock(LockType.READ)
    public double getBullScore(String symbol, int horizonDays) {
        return marketForecastService.getForecast(symbol, horizonDays).getBullScore();
    }

    @Schedule(hour = "*", minute = "*/15", second = "0", persistent = false)
    @Lock(LockType.READ)
    public void refreshMarketContexts() {
        marketContexts.refresh();
    }

    @Schedule(hour = "*", minute = "*", second = "*/10", persistent = false)
    @Lock(LockType.READ)
    public void reconnectTradeStreams() {
        binanceClientsBySymbol.forEach((symbol, client) -> {
            if (!client.isRunning()) {
                requestTradeStart(symbol);
            }
        });
    }

    @Lock(LockType.READ)
    public void updateMarketContext(String symbol, MarketContextUpdateRequest request) {
        marketContexts.update(symbol, request);
    }

    @Lock(LockType.READ)
    public AutoCloseable subscribeBars(Consumer<MarketBar> consumer) {
        return publisher.onBar(consumer);
    }

    @Lock(LockType.READ)
    public AutoCloseable subscribeSignals(Consumer<AiSignal> consumer) {
        return publisher.onSignal(consumer);
    }

    private List<AiSignal> generateSignalsFromRemoteBars(String normalizedSymbol, int limit) {
        marketContexts.get(normalizedSymbol);
        final int remoteLimit = Math.max(50, Math.min(500, limit * 20));
        final List<MarketBar> remoteBars = marketDataClient.fetchKlines(normalizedSymbol, ChartInterval.parse("1MIN"), remoteLimit);
        if (remoteBars.isEmpty()) {
            return List.of();
        }

        final FeatureEngine remoteFeatureEngine = new FeatureEngine(marketContexts.registry());
        final AiSignalEngine remoteSignalEngine = new AiSignalEngine(configuration.getScoringSettings());
        final List<AiSignal> generatedSignals = remoteBars.stream()
            .map(bar -> enrichWithSignalBullScore(remoteFeatureEngine.onClosedBar(bar)))
            .map(remoteSignalEngine::evaluate)
            .filter(signal -> signal != null)
            .collect(Collectors.toList());
        return MarketHistoryBuffer.takeLast(generatedSignals, limit);
    }

    @Lock(LockType.READ)
    public void onTrade(MarketTrade trade) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(trade.getSymbol());
        final BarAggregator symbolBarAggregator = barAggregatorsBySymbol.computeIfAbsent(normalizedSymbol, ignored -> new BarAggregator(1_000L));
        final FeatureEngine symbolFeatureEngine = featureEnginesBySymbol.computeIfAbsent(normalizedSymbol, ignored -> new FeatureEngine(marketContexts.registry()));
        final AiSignalEngine symbolSignalEngine = signalEnginesBySymbol.computeIfAbsent(
            normalizedSymbol,
            ignored -> new AiSignalEngine(configuration.getScoringSettings())
        );
        final MarketBar closed = symbolBarAggregator.ingest(trade);
        final MarketBar forming = symbolBarAggregator.snapshotForming();
        if (forming != null) {
            publisher.publishBar(forming);
        }

        if (closed == null) {
            return;
        }

        history.appendBar(closed);
        barStorageService.storeAsync(closed);

        final FeatureSnapshot features = enrichWithSignalBullScore(symbolFeatureEngine.onClosedBar(closed));
        final AiSignal signal = symbolSignalEngine.evaluate(features);
        if (signal == null) {
            return;
        }

        history.appendSignal(signal);
        publisher.publishSignal(signal);
    }

    private FeatureSnapshot enrichWithSignalBullScore(FeatureSnapshot features) {
        if (features == null || !configuration.isSignalBullScoreEnabled()) {
            return features;
        }

        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(features.getSymbol());
        final Double bullScore = marketForecastService.getCachedBullScoreOrRequestRefresh(
            normalizedSymbol,
            configuration.getSignalBullScoreHorizonDays(),
            configuration.getSignalBullScoreTtlMs()
        );
        return bullScore == null ? features : features.withForecastBullScore(bullScore);
    }

    private void requestTradeStart(String normalizedSymbol) {
        if (!tradeStartsInFlight.add(normalizedSymbol)) {
            return;
        }

        try {
            sessionContext.getBusinessObject(MarketAiService.class).startLiveSymbol(normalizedSymbol);
        } catch (RuntimeException ex) {
            tradeStartsInFlight.remove(normalizedSymbol);
        }
    }
}
