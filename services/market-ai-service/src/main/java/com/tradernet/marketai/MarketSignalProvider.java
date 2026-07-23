package com.tradernet.marketai;

import com.tradernet.marketai.model.AiSignal;
import jakarta.ejb.Local;

import java.util.List;

/**
 * Cross-module contract for generated market signal queries.
 */
@Local
public interface MarketSignalProvider {

    List<AiSignal> getSignals(String symbol, int limit);
}
