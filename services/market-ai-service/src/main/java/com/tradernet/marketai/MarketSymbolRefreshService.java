package com.tradernet.marketai;

import jakarta.ejb.Local;

/**
 * Internal managed boundary for asynchronous symbol-catalog refreshes.
 */
@Local
public interface MarketSymbolRefreshService {

    void refreshSymbols();
}
