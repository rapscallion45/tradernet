package com.tradernet.portfolio.dto;

import java.util.ArrayList;
import java.util.List;

public class PortfolioHistoryPointDto {

    private long timestamp;
    private double accountValue;
    private List<PortfolioHistoryEventDto> events = new ArrayList<>();

    public PortfolioHistoryPointDto() {
    }

    public PortfolioHistoryPointDto(long timestamp, double accountValue, List<PortfolioHistoryEventDto> events) {
        this.timestamp = timestamp;
        this.accountValue = accountValue;
        this.events = events == null ? new ArrayList<>() : events;
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public double getAccountValue() { return accountValue; }
    public void setAccountValue(double accountValue) { this.accountValue = accountValue; }
    public List<PortfolioHistoryEventDto> getEvents() { return events; }
    public void setEvents(List<PortfolioHistoryEventDto> events) {
        this.events = events == null ? new ArrayList<>() : events;
    }
}
