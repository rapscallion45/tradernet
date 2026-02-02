package com.tradernet.model;

import java.time.LocalDateTime;

/**
 * Represents a completed trade in the trading system.
 * Stores symbol, quantity, price, and execution timestamp.
 */
public class Trade {
    private final String symbol;
    private final double quantity;
    private final double price;
    private final LocalDateTime timestamp;

    /**
     * Creates a new Trade.
     *
     * @param symbol   traded symbol
     * @param quantity number of shares
     * @param price    execution price
     */
    public Trade(String symbol, double quantity, double price) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = LocalDateTime.now();
    }

    public String getSymbol() {
        return symbol;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Trade{" + symbol + " " + quantity + " @ " + price + " on " + timestamp + "}";
    }
}
