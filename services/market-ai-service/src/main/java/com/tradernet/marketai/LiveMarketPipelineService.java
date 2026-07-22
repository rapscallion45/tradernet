package com.tradernet.marketai;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.context.MarketContextOperations;
import com.tradernet.marketai.engine.AiSignalEngine;
import com.tradernet.marketai.engine.BarAggregator;
import com.tradernet.marketai.engine.FeatureEngine;
import com.tradernet.marketai.engine.MarketEventPublisher;
import com.tradernet.marketai.forecast.MarketForecastProvider;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.marketai.model.MarketTrade;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns bounded, reference-counted live trade pipelines by symbol.
 */
@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LiveMarketPipelineService {

    private static final Logger LOG = LoggerFactory.getLogger(LiveMarketPipelineService.class);

    @EJB
    private MarketContextOperations marketContexts;

    @EJB
    private MarketAiConfiguration configuration;

    @EJB
    private MarketHistoryBuffer history;

    @EJB
    private MarketBarStorageService barStorageService;

    @EJB
    private MarketForecastProvider marketForecastProvider;

    @EJB
    private MarketEventPublisher publisher;

    @Resource
    private SessionContext sessionContext;

    private final Map<String, LiveSymbolRuntime> runtimesBySymbol = new HashMap<>();
    private final Set<String> startsInFlight = ConcurrentHashMap.newKeySet();
    private String pinnedDefaultSymbol;

    @PostConstruct
    public void start() {
        pinnedDefaultSymbol = MarketSymbolNormalizer.normalizeSymbol(configuration.getDefaultSymbol());
        marketContexts.registerSymbols(configuration.getContextSymbols());
        marketContexts.registerSymbol(pinnedDefaultSymbol);
        runtimesBySymbol.put(pinnedDefaultSymbol, createRuntime());
        requestTradeStart(pinnedDefaultSymbol);
        marketContexts.refresh();
    }

    @PreDestroy
    @Lock(LockType.WRITE)
    public void stop() {
        runtimesBySymbol.values().forEach(runtime -> runtime.client.stop());
        runtimesBySymbol.clear();
        startsInFlight.clear();
    }

    @Lock(LockType.WRITE)
    public String acquireSymbol(String symbol) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        LiveSymbolRuntime runtime = runtimesBySymbol.get(normalizedSymbol);
        if (runtime == null) {
            if (runtimesBySymbol.size() >= configuration.getMaxLiveSymbols()) {
                throw new MarketDataCapacityException("Live market capacity is currently exhausted");
            }
            runtime = createRuntime();
            runtimesBySymbol.put(normalizedSymbol, runtime);
        }
        runtime.references += 1;
        marketContexts.registerSymbol(normalizedSymbol);
        if (!runtime.client.isRunning()) {
            requestTradeStart(normalizedSymbol);
        }
        return normalizedSymbol;
    }

    @Lock(LockType.WRITE)
    public void releaseSymbol(String symbol) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final LiveSymbolRuntime runtime = runtimesBySymbol.get(normalizedSymbol);
        if (runtime == null) {
            return;
        }
        runtime.references = Math.max(0, runtime.references - 1);
        if (runtime.references == 0 && !normalizedSymbol.equals(pinnedDefaultSymbol)) {
            runtimesBySymbol.remove(normalizedSymbol);
            startsInFlight.remove(normalizedSymbol);
            runtime.client.stop();
        }
    }

    @Asynchronous
    @Lock(LockType.READ)
    public void startLiveSymbol(String normalizedSymbol) {
        try {
            final LiveSymbolRuntime runtime = runtimesBySymbol.get(normalizedSymbol);
            if (runtime != null && !runtime.client.isRunning()) {
                final LiveMarketPipelineService tradeHandler = sessionContext
                    .getBusinessObject(LiveMarketPipelineService.class);
                runtime.client.start(normalizedSymbol.toLowerCase(Locale.ROOT), tradeHandler::onTrade);
            }
        } finally {
            startsInFlight.remove(normalizedSymbol);
        }
    }

    @Schedule(hour = "*", minute = "*", second = "*/10", persistent = false)
    @Lock(LockType.READ)
    public void reconnectTradeStreams() {
        runtimesBySymbol.forEach((symbol, runtime) -> {
            if (!runtime.client.isRunning()) {
                requestTradeStart(symbol);
            }
        });
    }

    @Lock(LockType.READ)
    public void onTrade(MarketTrade trade) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(trade.getSymbol());
        final LiveSymbolRuntime runtime = runtimesBySymbol.get(normalizedSymbol);
        if (runtime == null) {
            return;
        }

        final MarketBar closed = runtime.barAggregator.ingest(trade);
        final MarketBar forming = runtime.barAggregator.snapshotForming();
        if (forming != null) {
            publisher.publishBar(forming);
        }
        if (closed == null) {
            return;
        }

        history.appendBar(closed);
        barStorageService.storeAsync(closed);
        final FeatureSnapshot features = enrichWithSignalBullScore(runtime.featureEngine.onClosedBar(closed));
        final AiSignal signal = runtime.signalEngine.evaluate(features);
        if (signal != null) {
            history.appendSignal(signal);
            publisher.publishSignal(signal);
        }
    }

    private LiveSymbolRuntime createRuntime() {
        return new LiveSymbolRuntime(
            new BinanceTradeStreamClient(configuration.getBinanceWebSocketBaseUrl()),
            new BarAggregator(1_000L),
            new FeatureEngine(marketContexts::enrich),
            new AiSignalEngine(configuration.getScoringSettings())
        );
    }

    private FeatureSnapshot enrichWithSignalBullScore(FeatureSnapshot features) {
        if (features == null || !configuration.isSignalBullScoreEnabled()) {
            return features;
        }
        final Double bullScore = marketForecastProvider.getCachedBullScoreOrRequestRefresh(
            MarketSymbolNormalizer.normalizeSymbol(features.getSymbol()),
            configuration.getSignalBullScoreHorizonDays(),
            configuration.getSignalBullScoreTtlMs()
        );
        return bullScore == null ? features : features.withForecastBullScore(bullScore);
    }

    private void requestTradeStart(String normalizedSymbol) {
        if (!startsInFlight.add(normalizedSymbol)) {
            return;
        }
        try {
            sessionContext.getBusinessObject(LiveMarketPipelineService.class).startLiveSymbol(normalizedSymbol);
        } catch (RuntimeException ex) {
            startsInFlight.remove(normalizedSymbol);
            LOG.warn("Unable to schedule the live market stream for {}. The reconnect task will retry.", normalizedSymbol, ex);
        }
    }

    private static final class LiveSymbolRuntime {
        private final BinanceTradeStreamClient client;
        private final BarAggregator barAggregator;
        private final FeatureEngine featureEngine;
        private final AiSignalEngine signalEngine;
        private int references;

        private LiveSymbolRuntime(
            BinanceTradeStreamClient client,
            BarAggregator barAggregator,
            FeatureEngine featureEngine,
            AiSignalEngine signalEngine
        ) {
            this.client = client;
            this.barAggregator = barAggregator;
            this.featureEngine = featureEngine;
            this.signalEngine = signalEngine;
        }
    }
}
