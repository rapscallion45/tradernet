package com.tradernet.marketai;

import jakarta.ejb.Local;

import java.util.List;

/**
 * Cross-module contract for exchange-supported symbol discovery.
 */
@Local
public interface MarketSymbolProvider {

    List<String> getSupportedSymbols(String quoteCurrency);
}
