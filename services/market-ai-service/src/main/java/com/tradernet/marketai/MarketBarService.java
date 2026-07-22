package com.tradernet.marketai;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.model.ChartInterval;
import com.tradernet.marketai.model.MarketBar;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Owns historical bar queries and cached latest-price reads.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketBarService implements MarketBarProvider {

    @EJB
    private BinanceMarketDataClient marketDataClient;

    @EJB
    private MarketHistoryBuffer history;

    @Override
    public List<MarketBar> getBars(String symbol, String intervalToken, int limit) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final ChartInterval interval = ChartInterval.parse(intervalToken);
        final List<MarketBar> remoteBars = marketDataClient.fetchKlines(normalizedSymbol, interval, limit);
        return remoteBars.isEmpty() ? history.getBarsForSymbol(normalizedSymbol, limit) : remoteBars;
    }

    @Override
    public OptionalDouble getLatestCachedPrice(String symbol) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final List<MarketBar> bars = history.getBarsForSymbol(normalizedSymbol, 1);
        if (bars.isEmpty() || bars.get(0) == null || bars.get(0).getClose() <= 0.0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(bars.get(0).getClose());
    }
}
