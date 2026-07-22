package com.tradernet.marketai.context;

import jakarta.ejb.Local;

/**
 * Internal managed boundary for asynchronous context hydration.
 */
@Local
public interface MarketContextRefreshService {

    void hydrateAsync(String symbol);
}
