package com.tradernet.order.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Response payload for order data and computed performance metrics.
 */
public class OrderResponseDto implements Serializable {
    private long id;
    private long userId;
    private String symbol;
    private String side;
    private String currency;
    private Double quantity;
    private Double price;
    private String status;
    private Instant createdAt;
    private Instant closedAt;
    private Double currentPrice;
    private Double pnl;
    private Double pnlPercent;
    private String timing;
    private String aiPrediction;
    private Double bullScore;
    private Double closePrice;
    private Double netValue;

    public OrderResponseDto() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }
    public Double getPnl() { return pnl; }
    public void setPnl(Double pnl) { this.pnl = pnl; }
    public Double getPnlPercent() { return pnlPercent; }
    public void setPnlPercent(Double pnlPercent) { this.pnlPercent = pnlPercent; }
    public String getTiming() { return timing; }
    public void setTiming(String timing) { this.timing = timing; }
    public String getAiPrediction() { return aiPrediction; }
    public void setAiPrediction(String aiPrediction) { this.aiPrediction = aiPrediction; }
    public Double getBullScore() { return bullScore; }
    public void setBullScore(Double bullScore) { this.bullScore = bullScore; }
    public Double getClosePrice() { return closePrice; }
    public void setClosePrice(Double closePrice) { this.closePrice = closePrice; }
    public Double getNetValue() { return netValue; }
    public void setNetValue(Double netValue) { this.netValue = netValue; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderResponseDto that = (OrderResponseDto) o;
        return id == that.id && userId == that.userId && Objects.equals(symbol, that.symbol)
            && Objects.equals(side, that.side) && Objects.equals(currency, that.currency)
            && Objects.equals(quantity, that.quantity) && Objects.equals(price, that.price)
            && Objects.equals(status, that.status) && Objects.equals(createdAt, that.createdAt)
            && Objects.equals(closedAt, that.closedAt)
            && Objects.equals(currentPrice, that.currentPrice)
            && Objects.equals(pnl, that.pnl)
            && Objects.equals(pnlPercent, that.pnlPercent)
            && Objects.equals(timing, that.timing)
            && Objects.equals(aiPrediction, that.aiPrediction)
            && Objects.equals(bullScore, that.bullScore)
            && Objects.equals(closePrice, that.closePrice)
            && Objects.equals(netValue, that.netValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, symbol, side, currency, quantity, price, status, createdAt, closedAt, currentPrice,
            pnl, pnlPercent, timing, aiPrediction, bullScore, closePrice, netValue);
    }
}
