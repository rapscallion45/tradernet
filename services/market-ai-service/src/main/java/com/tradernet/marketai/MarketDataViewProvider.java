package com.tradernet.marketai;

import com.tradernet.marketai.model.MarketBar;
import com.tradernet.marketai.orderbook.OrderBookSnapshot;
import jakarta.ejb.Local;

import java.util.List;

/**
 * Cross-module contract for display-currency market views.
 */
@Local
public interface MarketDataViewProvider {

    List<MarketBar> getBars(String symbol, String interval, int limit, String currency);

    MarketBar convertBar(MarketBar bar, String currency);

    OrderBookSnapshot getOrderBook(String symbol, int levels, String currency);
}
