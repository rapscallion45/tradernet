package com.tradernet.marketai;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.context.MarketContextOperations;
import com.tradernet.marketai.engine.AiSignalEngine;
import com.tradernet.marketai.engine.FeatureEngine;
import com.tradernet.marketai.forecast.MarketForecastProvider;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.ChartInterval;
import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.MarketBar;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds signal query results from live history or remote bars.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketSignalQueryService implements MarketSignalProvider {

    @EJB
    private MarketHistoryBuffer history;

    @EJB
    private BinanceMarketDataClient marketDataClient;

    @EJB
    private MarketContextOperations marketContexts;

    @EJB
    private MarketForecastProvider marketForecastProvider;

    @EJB
    private MarketAiConfiguration configuration;

    @Override
    public List<AiSignal> getSignals(String symbol, int limit) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final List<AiSignal> matchingSignals = history.getSignalsForSymbol(normalizedSymbol, limit);
        return matchingSignals.isEmpty()
            ? generateSignalsFromRemoteBars(normalizedSymbol, limit)
            : matchingSignals;
    }

    private List<AiSignal> generateSignalsFromRemoteBars(String normalizedSymbol, int limit) {
        marketContexts.get(normalizedSymbol);
        final int remoteLimit = Math.max(50, Math.min(500, limit * 20));
        final List<MarketBar> remoteBars = marketDataClient.fetchKlines(
            normalizedSymbol,
            ChartInterval.parse("1MIN"),
            remoteLimit
        );
        if (remoteBars.isEmpty()) {
            return List.of();
        }

        final FeatureEngine featureEngine = new FeatureEngine(marketContexts::enrich);
        final AiSignalEngine signalEngine = new AiSignalEngine(configuration.getScoringSettings());
        final List<AiSignal> generatedSignals = remoteBars.stream()
            .map(bar -> enrichWithSignalBullScore(featureEngine.onClosedBar(bar)))
            .map(signalEngine::evaluate)
            .filter(signal -> signal != null)
            .collect(Collectors.toList());
        return MarketHistoryBuffer.takeLast(generatedSignals, limit);
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
}
