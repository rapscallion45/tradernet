package com.tradernet.marketai.orderbook;

/**
 * Aggregated L2 price level prepared for API clients.
 */
public class OrderBookLevel {

    private final double price;
    private final double quantity;
    private final double notional;
    private final double cumulativeQuantity;
    private final double cumulativeNotional;
    private final double depthPercent;

    public OrderBookLevel(double price, double quantity, double notional, double cumulativeQuantity, double cumulativeNotional, double depthPercent) {
        this.price = price;
        this.quantity = quantity;
        this.notional = notional;
        this.cumulativeQuantity = cumulativeQuantity;
        this.cumulativeNotional = cumulativeNotional;
        this.depthPercent = depthPercent;
    }

    public double getPrice() {
        return price;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getNotional() {
        return notional;
    }

    public double getCumulativeQuantity() {
        return cumulativeQuantity;
    }

    public double getCumulativeNotional() {
        return cumulativeNotional;
    }

    public double getDepthPercent() {
        return depthPercent;
    }
}
