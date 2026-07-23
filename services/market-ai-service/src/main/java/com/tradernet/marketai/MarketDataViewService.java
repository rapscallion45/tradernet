package com.tradernet.marketai;

import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.currencyconversion.CurrencyConversionProvider;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.marketai.orderbook.OrderBookLevel;
import com.tradernet.marketai.orderbook.OrderBookSnapshot;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides display-currency views of market data.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketDataViewService implements MarketDataViewProvider {

    private static final MathContext MC = MathContext.DECIMAL64;

    @EJB
    private MarketBarProvider marketBarProvider;

    @EJB
    private com.tradernet.marketai.orderbook.MarketOrderBookService marketOrderBookService;

    @EJB
    private CurrencyConversionProvider currencyConversionService;

    @Override
    public List<MarketBar> getBars(String symbol, String interval, int limit, String currency) {
        final CurrencyCode targetCurrency = CurrencyCode.parseOrDefault(currency, CurrencyCode.USD);
        final List<MarketBar> bars = marketBarProvider.getBars(symbol, interval, limit);
        prefetchBarRates(bars, targetCurrency);
        return bars.stream()
            .map(bar -> convertBar(bar, targetCurrency))
            .collect(Collectors.toList());
    }

    @Override
    public MarketBar convertBar(MarketBar bar, String currency) {
        return convertBar(bar, CurrencyCode.parseOrDefault(currency, CurrencyCode.USD));
    }

    @Override
    public OrderBookSnapshot getOrderBook(String symbol, int levels, String currency) {
        final CurrencyCode targetCurrency = CurrencyCode.parseOrDefault(currency, CurrencyCode.USD);
        return convertOrderBook(marketOrderBookService.getOrderBook(symbol, levels), targetCurrency);
    }

    private MarketBar convertBar(MarketBar bar, CurrencyCode targetCurrency) {
        if (bar == null) {
            return null;
        }

        CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(bar.getSymbol());
        Instant timestamp = Instant.ofEpochMilli(bar.getBucketStart());

        if (sourceCurrency == targetCurrency) {
            return bar;
        }

        return new MarketBar(
            bar.getSymbol(),
            bar.getBucketStart(),
            currencyConversionService.convertAmount(bar.getOpen(), sourceCurrency, targetCurrency, timestamp),
            currencyConversionService.convertAmount(bar.getHigh(), sourceCurrency, targetCurrency, timestamp),
            currencyConversionService.convertAmount(bar.getLow(), sourceCurrency, targetCurrency, timestamp),
            currencyConversionService.convertAmount(bar.getClose(), sourceCurrency, targetCurrency, timestamp),
            bar.getVolume(),
            bar.isClosed()
        );
    }

    private void prefetchBarRates(List<MarketBar> bars, CurrencyCode targetCurrency) {
        if (bars == null || bars.isEmpty()) {
            return;
        }

        final MarketBar firstBar = bars.stream().filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (firstBar == null) {
            return;
        }
        final CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(firstBar.getSymbol());
        if (sourceCurrency == targetCurrency) {
            return;
        }

        final long firstTimestamp = bars.stream()
            .filter(java.util.Objects::nonNull)
            .mapToLong(MarketBar::getBucketStart)
            .min()
            .orElse(firstBar.getBucketStart());
        final long lastTimestamp = bars.stream()
            .filter(java.util.Objects::nonNull)
            .mapToLong(MarketBar::getBucketStart)
            .max()
            .orElse(firstBar.getBucketStart());
        currencyConversionService.prefetchRates(
            sourceCurrency,
            targetCurrency,
            Instant.ofEpochMilli(firstTimestamp),
            Instant.ofEpochMilli(lastTimestamp)
        );
    }

    private OrderBookSnapshot convertOrderBook(OrderBookSnapshot snapshot, CurrencyCode targetCurrency) {
        if (snapshot == null) {
            return null;
        }

        CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(snapshot.getSymbol());
        Instant timestamp = snapshot.getEventTime() > 0L ? Instant.ofEpochMilli(snapshot.getEventTime()) : Instant.now();

        if (sourceCurrency == targetCurrency) {
            return snapshot;
        }

        return new OrderBookSnapshot(
            snapshot.getSymbol(),
            targetCurrency.name(),
            snapshot.getStatus(),
            snapshot.getSource(),
            snapshot.getAggregation(),
            snapshot.getMessage(),
            snapshot.getEventTime(),
            snapshot.getLastUpdateId(),
            snapshot.getUpdateLatencyMs(),
            snapshot.getResyncCount(),
            snapshot.getExchangeSnapshotLimit(),
            snapshot.getRequestedLevels(),
            snapshot.isStale(),
            convertMarketAmount(snapshot.getBestBid(), sourceCurrency, targetCurrency, timestamp),
            convertMarketAmount(snapshot.getBestAsk(), sourceCurrency, targetCurrency, timestamp),
            convertMarketAmount(snapshot.getMidPrice(), sourceCurrency, targetCurrency, timestamp),
            convertMarketAmount(snapshot.getSpread(), sourceCurrency, targetCurrency, timestamp),
            snapshot.getSpreadPercent(),
            convertMarketAmount(snapshot.getBidDepthNotional(), sourceCurrency, targetCurrency, timestamp),
            convertMarketAmount(snapshot.getAskDepthNotional(), sourceCurrency, targetCurrency, timestamp),
            snapshot.getDepthImbalancePercent(),
            convertOrderBookLevels(snapshot.getBids(), sourceCurrency, targetCurrency, timestamp),
            convertOrderBookLevels(snapshot.getAsks(), sourceCurrency, targetCurrency, timestamp)
        );
    }

    private List<OrderBookLevel> convertOrderBookLevels(List<OrderBookLevel> levels, CurrencyCode from, CurrencyCode to, Instant timestamp) {
        List<OrderBookLevel> converted = new ArrayList<>(levels.size());
        for (OrderBookLevel level : levels) {
            converted.add(new OrderBookLevel(
                convertMarketAmount(level.getPrice(), from, to, timestamp),
                level.getQuantity(),
                convertMarketAmount(level.getNotional(), from, to, timestamp),
                level.getCumulativeQuantity(),
                convertMarketAmount(level.getCumulativeNotional(), from, to, timestamp),
                level.getDepthPercent()
            ));
        }
        return converted;
    }

    private double convertMarketAmount(double amount, CurrencyCode from, CurrencyCode to, Instant timestamp) {
        if (from == to) {
            return amount;
        }

        BigDecimal rate = currencyConversionService.getRate(from, to, timestamp);
        return BigDecimal.valueOf(amount).multiply(rate, MC).doubleValue();
    }
}
