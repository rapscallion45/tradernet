package com.tradernet.marketai.forecast;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.MarketAiConfiguration;
import com.tradernet.marketai.context.MarketContextOperations;
import com.tradernet.marketai.model.MarketContextSnapshot;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides stale-while-revalidate forecast reads without blocking request threads on model services.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketForecastService implements MarketForecastProvider, MarketForecastRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(MarketForecastService.class);

    @EJB
    private MarketContextOperations marketContexts;

    @EJB
    private ForecastingClient forecastingClient;

    @EJB
    private OllamaNarrativeClient narrativeClient;

    @EJB
    private MarketAiConfiguration configuration;

    @Resource
    private SessionContext sessionContext;

    private final Map<String, CachedForecast> forecastsByKey = new ConcurrentHashMap<>();
    private final Set<String> refreshesInFlight = ConcurrentHashMap.newKeySet();

    @Override
    public MarketForecast getForecast(String symbol, int horizonDays) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final int boundedHorizon = Math.max(1, Math.min(horizonDays, 365));
        final String cacheKey = cacheKey(normalizedSymbol, boundedHorizon);
        final CachedForecast cached = forecastsByKey.get(cacheKey);

        if (!isFresh(cached, configuration.getForecastTtlMs())) {
            requestRefresh(normalizedSymbol, boundedHorizon, cacheKey);
        }

        return cached == null
            ? MarketForecast.unavailable(normalizedSymbol, boundedHorizon, "forecast_refresh_pending")
            : cached.forecast.copy();
    }

    @Override
    public Double getCachedBullScoreOrRequestRefresh(String symbol, int horizonDays, long ttlMs) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final int boundedHorizon = Math.max(1, Math.min(horizonDays, 365));
        final String cacheKey = cacheKey(normalizedSymbol, boundedHorizon);
        final CachedForecast cached = forecastsByKey.get(cacheKey);
        if (!isFresh(cached, ttlMs)) {
            requestRefresh(normalizedSymbol, boundedHorizon, cacheKey);
        }
        return cached == null ? null : cached.forecast.getBullScore();
    }

    @Asynchronous
    @Override
    public void refreshForecast(String normalizedSymbol, int horizonDays) {
        final String cacheKey = cacheKey(normalizedSymbol, horizonDays);
        try {
            final MarketContextSnapshot context = marketContexts.get(normalizedSymbol);
            final MarketForecast forecast = forecastingClient.forecast(normalizedSymbol, horizonDays, context);
            forecast.setNarrative(narrativeClient.summarize(forecast));
            forecastsByKey.put(cacheKey, new CachedForecast(forecast.copy(), System.currentTimeMillis()));
        } catch (RuntimeException ex) {
            LOG.warn("Unable to refresh forecast for {} over {} days.", normalizedSymbol, horizonDays, ex);
        } finally {
            refreshesInFlight.remove(cacheKey);
        }
    }

    private void requestRefresh(String normalizedSymbol, int horizonDays, String cacheKey) {
        if (normalizedSymbol.isBlank() || !refreshesInFlight.add(cacheKey)) {
            return;
        }

        try {
            sessionContext.getBusinessObject(MarketForecastRefreshService.class)
                .refreshForecast(normalizedSymbol, horizonDays);
        } catch (RuntimeException ex) {
            refreshesInFlight.remove(cacheKey);
            LOG.warn("Unable to schedule forecast refresh for {} over {} days.", normalizedSymbol, horizonDays, ex);
        }
    }

    private boolean isFresh(CachedForecast cached, long ttlMs) {
        return cached != null && System.currentTimeMillis() - cached.refreshedAtMs < Math.max(1L, ttlMs);
    }

    private String cacheKey(String symbol, int horizonDays) {
        return symbol + ":" + horizonDays;
    }

    private static final class CachedForecast {
        private final MarketForecast forecast;
        private final long refreshedAtMs;

        private CachedForecast(MarketForecast forecast, long refreshedAtMs) {
            this.forecast = forecast;
            this.refreshedAtMs = refreshedAtMs;
        }
    }
}
