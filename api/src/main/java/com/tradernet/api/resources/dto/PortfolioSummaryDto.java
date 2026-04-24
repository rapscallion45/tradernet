package com.tradernet.api.resources.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Portfolio summary payload.
 */
public class PortfolioSummaryDto {

    private String currency;
    private double totalMarketValue;
    private double totalCost;
    private double totalProfitLoss;
    private double totalProfitLossPercent;
    private List<PortfolioAssetDto> assets = new ArrayList<>();
    private List<PortfolioHistoryPointDto> history = new ArrayList<>();

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getTotalMarketValue() {
        return totalMarketValue;
    }

    public void setTotalMarketValue(double totalMarketValue) {
        this.totalMarketValue = totalMarketValue;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public double getTotalProfitLoss() {
        return totalProfitLoss;
    }

    public void setTotalProfitLoss(double totalProfitLoss) {
        this.totalProfitLoss = totalProfitLoss;
    }

    public double getTotalProfitLossPercent() {
        return totalProfitLossPercent;
    }

    public void setTotalProfitLossPercent(double totalProfitLossPercent) {
        this.totalProfitLossPercent = totalProfitLossPercent;
    }

    public List<PortfolioAssetDto> getAssets() {
        return assets;
    }

    public void setAssets(List<PortfolioAssetDto> assets) {
        this.assets = assets;
    }

    public List<PortfolioHistoryPointDto> getHistory() {
        return history;
    }

    public void setHistory(List<PortfolioHistoryPointDto> history) {
        this.history = history;
    }
}
