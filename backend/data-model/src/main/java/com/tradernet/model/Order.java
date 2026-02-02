package com.tradernet.model;

/**
 * Represents a single order in the trading system.
 * An order can be BUY or SELL, and includes symbol, quantity, and price.
 */
public class Order {

    private final String symbol;
    private final Side side;
    private final double quantity;
    private final double price;

    /**
     * Constructs a new Order.
     *
     * @param symbol   the stock symbol (e.g., "AAPL")
     * @param side     BUY or SELL
     * @param quantity number of shares
     * @param price    price per share
     */
    public Order(String symbol, int quantity, double price, Side side) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
    }

    public String getSymbol() {
        return symbol;
    }

    public Side getSide() {
        return side;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return side + " " + quantity + " " + symbol + " @ " + price;
    }

    /**
     * BUY for purchase orders, SELL for sell orders.
     */
    public enum Side {BUY, SELL}
}
