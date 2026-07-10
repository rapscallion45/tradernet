package com.tradernet.jpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Represents a completed trade in the trading system.
 * Stores user/order ownership, symbol, signed quantity, price, and execution timestamp.
 */
@Entity
@Table(name = "tblTrades")
public class TradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long orderId;
    private String symbol;
    private String side;
    private String executionType;
    private double quantity;
    private double price;
    private LocalDateTime timestamp;

    public TradeEntity() {
    }

    /**
     * Creates a new Trade.
     *
     * @param symbol   traded symbol
     * @param quantity number of shares
     * @param price    execution price
     */
    public TradeEntity(String symbol, double quantity, double price) {
        this(null, null, symbol, null, null, quantity, price);
    }

    public TradeEntity(Long userId, Long orderId, String symbol, String side, String executionType, double quantity, double price) {
        this.userId = userId;
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.executionType = executionType;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getExecutionType() {
        return executionType;
    }

    public void setExecutionType(String executionType) {
        this.executionType = executionType;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Trade{" + symbol + " " + quantity + " @ " + price + " on " + timestamp + "}";
    }
}
