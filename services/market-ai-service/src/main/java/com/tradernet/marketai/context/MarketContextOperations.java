package com.tradernet.marketai.context;

import com.tradernet.marketai.model.MarketContextSnapshot;
import com.tradernet.marketai.model.MarketContextUpdateRequest;
import com.tradernet.marketai.model.FeatureSnapshot;
import jakarta.ejb.Local;

/**
 * Contract for market-context registration, reads, updates, and refresh scheduling.
 */
@Local
public interface MarketContextOperations {

    FeatureSnapshot enrich(FeatureSnapshot features);

    void registerSymbol(String symbol);

    void registerSymbols(String symbols);

    MarketContextSnapshot get(String symbol);

    void refresh();

    void update(String symbol, MarketContextUpdateRequest request);
}
