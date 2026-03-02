package com.tradernet.order.dto;

import com.tradernet.jpa.entities.OrderEntity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class OrderResponseDto implements Serializable {
    private long orderId;
    private long userId;
    private String symbol;
    private String side;
    private Double quantity;
    private Double price;
    private String status;
    private Instant createdAt;
    private Double currentPrice;
    private Double pnl;
    private Double pnlPercent;
    private String timing;

    public OrderResponseDto() {
    }

    public static OrderResponseDto fromOrder(OrderEntity order) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.orderId = order.getId() == null ? 0L : order.getId();
        dto.userId = order.getUserId() == null ? 0L : order.getUserId();
        dto.symbol = order.getSymbol();
        dto.side = order.getSide() == null ? null : order.getSide().name();
        dto.quantity = order.getQuantity();
        dto.price = order.getPrice();
        dto.status = order.getStatus();
        dto.createdAt = order.getCreatedAt();
        return dto;
    }

    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }
    public Double getPnl() { return pnl; }
    public void setPnl(Double pnl) { this.pnl = pnl; }
    public Double getPnlPercent() { return pnlPercent; }
    public void setPnlPercent(Double pnlPercent) { this.pnlPercent = pnlPercent; }
    public String getTiming() { return timing; }
    public void setTiming(String timing) { this.timing = timing; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderResponseDto that = (OrderResponseDto) o;
        return orderId == that.orderId && userId == that.userId && Objects.equals(symbol, that.symbol) && Objects.equals(side, that.side)
            && Objects.equals(quantity, that.quantity) && Objects.equals(price, that.price) && Objects.equals(status, that.status)
            && Objects.equals(createdAt, that.createdAt) && Objects.equals(currentPrice, that.currentPrice)
            && Objects.equals(pnl, that.pnl) && Objects.equals(pnlPercent, that.pnlPercent) && Objects.equals(timing, that.timing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, userId, symbol, side, quantity, price, status, createdAt, currentPrice, pnl, pnlPercent, timing);
    }
}
