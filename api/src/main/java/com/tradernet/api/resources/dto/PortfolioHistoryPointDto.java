package com.tradernet.api.resources.dto;

/**
 * Single account value datapoint for the portfolio chart.
 */
public class PortfolioHistoryPointDto {

    private long timestamp;
    private double accountValue;

    public PortfolioHistoryPointDto() {
    }

    public PortfolioHistoryPointDto(long timestamp, double accountValue) {
        this.timestamp = timestamp;
        this.accountValue = accountValue;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getAccountValue() {
        return accountValue;
    }

    public void setAccountValue(double accountValue) {
        this.accountValue = accountValue;
    }
}
