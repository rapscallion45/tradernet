package com.tradernet.marketai.model;

/**
 * Normalized trade event emitted from a market data source.
 */
public class MarketTrade {

    private final String symbol;
    private final long eventTime;
    private final double price;
    private final double quantity;

    public MarketTrade(String symbol, long eventTime, double price, double quantity) {
        this.symbol = symbol;
        this.eventTime = eventTime;
        this.price = price;
        this.quantity = quantity;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getEventTime() {
        return eventTime;
    }

    public double getPrice() {
        return price;
    }

    public double getQuantity() {
        return quantity;
    }
}
