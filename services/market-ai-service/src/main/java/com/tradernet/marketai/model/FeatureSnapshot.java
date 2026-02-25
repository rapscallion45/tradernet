package com.tradernet.marketai.model;

/**
 * Incremental feature vector derived from closed bars.
 */
public class FeatureSnapshot {

    private final String symbol;
    private final long eventTime;
    private final double close;
    private final double emaFast;
    private final double emaSlow;
    private final double rsi;

    public FeatureSnapshot(String symbol, long eventTime, double close, double emaFast, double emaSlow, double rsi) {
        this.symbol = symbol;
        this.eventTime = eventTime;
        this.close = close;
        this.emaFast = emaFast;
        this.emaSlow = emaSlow;
        this.rsi = rsi;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getEventTime() {
        return eventTime;
    }

    public double getClose() {
        return close;
    }

    public double getEmaFast() {
        return emaFast;
    }

    public double getEmaSlow() {
        return emaSlow;
    }

    public double getRsi() {
        return rsi;
    }
}
