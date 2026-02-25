package com.tradernet.marketai.engine;

import com.tradernet.marketai.model.MarketBar;
import com.tradernet.marketai.model.MarketTrade;

/**
 * Builds one-second bars from trade events.
 */
public class BarAggregator {

    private final long timeframeMs;
    private MarketBar forming;

    public BarAggregator(long timeframeMs) {
        this.timeframeMs = timeframeMs;
    }

    public synchronized MarketBar ingest(MarketTrade trade) {
        final long bucketStart = (trade.getEventTime() / timeframeMs) * timeframeMs;
        if (forming == null) {
            forming = new MarketBar(trade.getSymbol(), bucketStart, trade.getPrice(), trade.getPrice(), trade.getPrice(), trade.getPrice(), trade.getQuantity(), false);
            return null;
        }

        if (bucketStart > forming.getBucketStart()) {
            final MarketBar closed = new MarketBar(forming.getSymbol(), forming.getBucketStart(), forming.getOpen(), forming.getHigh(), forming.getLow(), forming.getClose(), forming.getVolume(), true);
            forming = new MarketBar(trade.getSymbol(), bucketStart, trade.getPrice(), trade.getPrice(), trade.getPrice(), trade.getPrice(), trade.getQuantity(), false);
            return closed;
        }

        if (bucketStart < forming.getBucketStart()) {
            return null;
        }

        final double high = Math.max(forming.getHigh(), trade.getPrice());
        final double low = Math.min(forming.getLow(), trade.getPrice());
        final double volume = forming.getVolume() + trade.getQuantity();
        forming = new MarketBar(forming.getSymbol(), forming.getBucketStart(), forming.getOpen(), high, low, trade.getPrice(), volume, false);
        return null;
    }

    public synchronized MarketBar snapshotForming() {
        return forming;
    }
}
