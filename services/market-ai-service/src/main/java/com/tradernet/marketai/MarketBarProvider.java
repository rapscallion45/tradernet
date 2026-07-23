package com.tradernet.marketai;

import com.tradernet.marketai.model.MarketBar;
import jakarta.ejb.Local;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Cross-module contract for market bar queries and cached prices.
 */
@Local
public interface MarketBarProvider {

    int MAX_BARS = 2_000;

    List<MarketBar> getBars(String symbol, String intervalToken, int limit);

    OptionalDouble getLatestCachedPrice(String symbol);
}
