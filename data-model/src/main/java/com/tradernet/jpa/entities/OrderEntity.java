package com.tradernet.jpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Represents a single order in the trading system.
 */
@Entity
@Table(name = "tblOrders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String symbol;

    @Enumerated(EnumType.STRING)
    private Side side;

    private double quantity;
    private double price;
    private String status;
    private Instant createdAt;

    public OrderEntity() {
    }

    /**
     * Constructs a new Order.
     *
     * @param symbol the trading symbol (e.g. BTCUSDT)
     * @param quantity quantity in base asset units
     * @param price entry price
     * @param side BUY or SELL
     */
    public OrderEntity(String symbol, double quantity, double price, Side side) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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
