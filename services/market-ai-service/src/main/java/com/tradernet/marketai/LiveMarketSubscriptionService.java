package com.tradernet.marketai;

import jakarta.ejb.Local;

/**
 * Cross-module contract for reference-counted, symbol-scoped live subscriptions.
 */
@Local
public interface LiveMarketSubscriptionService {

    String subscribe(String symbol, LiveMarketEventListener listener);

    void unsubscribe(String subscriptionId);
}
