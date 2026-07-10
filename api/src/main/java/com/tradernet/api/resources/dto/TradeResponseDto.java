package com.tradernet.api.resources.dto;

import com.tradernet.jpa.entities.TradeEntity;

import java.time.LocalDateTime;

/**
 * User-visible trade history item.
 */
public class TradeResponseDto {

    private Long id;
    private Long orderId;
    private String symbol;
    private String side;
    private String executionType;
    private double quantity;
    private double price;
    private LocalDateTime timestamp;

    public static TradeResponseDto fromTrade(TradeEntity trade) {
        TradeResponseDto dto = new TradeResponseDto();
        dto.setId(trade.getId());
        dto.setOrderId(trade.getOrderId());
        dto.setSymbol(trade.getSymbol());
        dto.setSide(trade.getSide());
        dto.setExecutionType(trade.getExecutionType());
        dto.setQuantity(trade.getQuantity());
        dto.setPrice(trade.getPrice());
        dto.setTimestamp(trade.getTimestamp());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
