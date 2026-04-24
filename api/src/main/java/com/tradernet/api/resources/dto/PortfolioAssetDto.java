package com.tradernet.api.resources.dto;

/**
 * Portfolio entry for a currently held asset.
 */
public class PortfolioAssetDto {

    private String symbol;
    private double quantity;
    private double averageCost;
    private double currentPrice;
    private double totalCost;
    private double marketValue;
    private double profitLoss;
    private double profitLossPercent;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getAverageCost() {
        return averageCost;
    }

    public void setAverageCost(double averageCost) {
        this.averageCost = averageCost;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public double getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(double marketValue) {
        this.marketValue = marketValue;
    }

    public double getProfitLoss() {
        return profitLoss;
    }

    public void setProfitLoss(double profitLoss) {
        this.profitLoss = profitLoss;
    }

    public double getProfitLossPercent() {
        return profitLossPercent;
    }

    public void setProfitLossPercent(double profitLossPercent) {
        this.profitLossPercent = profitLossPercent;
    }
}
