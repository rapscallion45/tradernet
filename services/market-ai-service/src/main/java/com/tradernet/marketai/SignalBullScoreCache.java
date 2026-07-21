package com.tradernet.marketai;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.context.MarketContextService;
import com.tradernet.marketai.forecast.ForecastingClient;
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Non-blocking cache for forecast bull scores used by live signal enrichment.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SignalBullScoreCache {

    @EJB
    private MarketContextService marketContexts;

    @EJB
    private ForecastingClient forecastingClient;

    @Resource
    private SessionContext sessionContext;

    private final Map<String, CachedScore> scoresByKey = new ConcurrentHashMap<>();
    private final Set<String> refreshesInFlight = ConcurrentHashMap.newKeySet();

    public Double getScoreOrRequestRefresh(String symbol, int horizonDays, long ttlMs) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final String cacheKey = cacheKey(normalizedSymbol, horizonDays);
        final CachedScore cachedScore = scoresByKey.get(cacheKey);
        if (isFresh(cachedScore, ttlMs)) {
            return cachedScore.score;
        }

        requestRefresh(normalizedSymbol, horizonDays, cacheKey);
        return cachedScore == null ? null : cachedScore.score;
    }

    private void requestRefresh(String normalizedSymbol, int horizonDays, String cacheKey) {
        if (normalizedSymbol.isBlank() || !refreshesInFlight.add(cacheKey)) {
            return;
        }

        try {
            sessionContext.getBusinessObject(SignalBullScoreCache.class)
                .refreshScore(normalizedSymbol, horizonDays);
        } catch (RuntimeException ex) {
            refreshesInFlight.remove(cacheKey);
        }
    }

    @Asynchronous
    public void refreshScore(String normalizedSymbol, int horizonDays) {
        final String cacheKey = cacheKey(normalizedSymbol, horizonDays);
        try {
            final MarketContextSnapshot snapshot = marketContexts.getHydrated(normalizedSymbol);
            final double bullScore = forecastingClient.forecast(normalizedSymbol, horizonDays, snapshot).getBullScore();
            scoresByKey.put(cacheKey, new CachedScore(bullScore, System.currentTimeMillis()));
        } catch (RuntimeException ex) {
            // Forecasting is advisory for live signals; callers keep using the previous cached score when available.
        } finally {
            refreshesInFlight.remove(cacheKey);
        }
    }

    private boolean isFresh(CachedScore cachedScore, long ttlMs) {
        return cachedScore != null && System.currentTimeMillis() - cachedScore.refreshedAtMs < Math.max(1L, ttlMs);
    }

    private String cacheKey(String normalizedSymbol, int horizonDays) {
        return normalizedSymbol + ":" + horizonDays;
    }

    private static class CachedScore {
        private final double score;
        private final long refreshedAtMs;

        private CachedScore(double score, long refreshedAtMs) {
            this.score = score;
            this.refreshedAtMs = refreshedAtMs;
        }
    }
}
