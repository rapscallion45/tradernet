package com.tradernet.marketai.model;

/**
 * OHLCV bar used by charting and feature engineering.
 */
public class MarketBar {

    private final String symbol;
    private final long bucketStart;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;
    private final boolean closed;

    public MarketBar(String symbol, long bucketStart, double open, double high, double low, double close, double volume, boolean closed) {
        this.symbol = symbol;
        this.bucketStart = bucketStart;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.closed = closed;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getBucketStart() {
        return bucketStart;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public double getVolume() {
        return volume;
    }

    public boolean isClosed() {
        return closed;
    }
}
