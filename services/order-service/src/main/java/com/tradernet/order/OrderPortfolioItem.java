package com.tradernet.order;

import com.tradernet.order.dto.OrderSide;

import java.time.Instant;

/**
 * Persistence-neutral order projection consumed by portfolio calculations.
 */
public final class OrderPortfolioItem {

    private final String symbol;
    private final OrderSide side;
    private final double quantity;
    private final double price;
    private final boolean closed;
    private final Instant createdAt;
    private final Instant closedAt;
    private final Double closePrice;

    public OrderPortfolioItem(
        String symbol,
        OrderSide side,
        double quantity,
        double price,
        boolean closed,
        Instant createdAt,
        Instant closedAt,
        Double closePrice
    ) {
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.closed = closed;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
        this.closePrice = closePrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public boolean isClosed() {
        return closed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Double getClosePrice() {
        return closePrice;
    }
}
