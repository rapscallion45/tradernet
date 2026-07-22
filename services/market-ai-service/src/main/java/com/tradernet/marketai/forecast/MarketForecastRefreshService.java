package com.tradernet.marketai.forecast;

import jakarta.ejb.Local;

/**
 * Internal managed boundary for asynchronous forecast refreshes.
 */
@Local
public interface MarketForecastRefreshService {

    void refreshForecast(String normalizedSymbol, int horizonDays);
}
