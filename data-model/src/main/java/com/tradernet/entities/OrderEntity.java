package com.tradernet.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents a single order in the trading system.
 * An order can be BUY or SELL, and includes symbol, quantity, and price.
 */
@Entity
@Table(name = "tblOrders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    @Enumerated(EnumType.STRING)
    private Side side;

    private double quantity;

    private double price;

    public OrderEntity() {
    }

    /**
     * Constructs a new Order.
     *
     * @param symbol   the stock symbol (e.g., "AAPL")
     * @param side     BUY or SELL
     * @param quantity number of shares
     * @param price    price per share
     */
    public OrderEntity(String symbol, int quantity, double price, Side side) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
    }

    public Long getId() {
        return id;
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
