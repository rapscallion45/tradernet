package com.tradernet.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Represents a completed trade in the trading system.
 * Stores symbol, quantity, price, and execution timestamp.
 */
@Entity
@Table(name = "trades")
public class TradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
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
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
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
