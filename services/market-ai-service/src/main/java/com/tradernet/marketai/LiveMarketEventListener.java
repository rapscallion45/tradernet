package com.tradernet.marketai;

import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.MarketBar;

/**
 * Local callback used by transport adapters consuming a live market subscription.
 */
public interface LiveMarketEventListener {

    void onBar(MarketBar bar);

    void onSignal(AiSignal signal);
}
