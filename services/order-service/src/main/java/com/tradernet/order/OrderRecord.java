package com.tradernet.order;

import com.tradernet.order.dto.OrderSide;

import java.time.Instant;

/**
 * Persistence-neutral service representation of an order.
 */
public final class OrderRecord {

    private final long id;
    private final long userId;
    private final String symbol;
    private final OrderSide side;
    private final double quantity;
    private final double price;
    private final OrderStatus status;
    private final Instant createdAt;
    private final Instant closedAt;
    private final Double closePrice;
    private final String aiPrediction;
    private final Double bullScore;

    public OrderRecord(
        long id,
        long userId,
        String symbol,
        OrderSide side,
        double quantity,
        double price,
        OrderStatus status,
        Instant createdAt,
        Instant closedAt,
        Double closePrice,
        String aiPrediction,
        Double bullScore
    ) {
        this.id = id;
        this.userId = userId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
        this.closePrice = closePrice;
        this.aiPrediction = aiPrediction;
        this.bullScore = bullScore;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
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

    public OrderStatus getStatus() {
        return status;
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

    public String getAiPrediction() {
        return aiPrediction;
    }

    public Double getBullScore() {
        return bullScore;
    }
}
