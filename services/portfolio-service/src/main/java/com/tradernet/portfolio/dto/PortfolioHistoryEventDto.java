package com.tradernet.portfolio.dto;

public class PortfolioHistoryEventDto {

    private String symbol;
    private String side;
    private double quantity;
    private double price;
    private long timestamp;

    public PortfolioHistoryEventDto() {
    }

    public PortfolioHistoryEventDto(String symbol, String side, double quantity, double price, long timestamp) {
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
