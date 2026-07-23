package com.tradernet.marketai.forecast;

import jakarta.ejb.Local;

/**
 * Cross-module contract for cache-first market forecasts.
 */
@Local
public interface MarketForecastProvider {

    MarketForecast getForecast(String symbol, int horizonDays);

    Double getCachedBullScoreOrRequestRefresh(String symbol, int horizonDays, long ttlMs);
}
