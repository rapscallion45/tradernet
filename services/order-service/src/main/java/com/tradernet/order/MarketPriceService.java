package com.tradernet.order;

import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.model.MarketBar;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.List;

/**
 * Resolves latest market prices with caller-provided fallback values.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketPriceService {

    @EJB
    private MarketAiService marketAiService;

    public double resolveCurrentPrice(String symbol, double fallbackPrice) {
        final List<MarketBar> bars = marketAiService.getBars(symbol, "1S", 1);
        if (bars == null || bars.isEmpty() || bars.get(0) == null || bars.get(0).getClose() <= 0.0) {
            return fallbackPrice;
        }
        return bars.get(0).getClose();
    }
}
