package com.tradernet.marketai.forecast;

import com.tradernet.marketai.model.ExplanationItem;

import java.util.Collections;
import java.util.List;

/**
 * Forecast summary returned by the Python forecasting service and enriched by Ollama.
 */
public class MarketForecast {

    private String symbol;
    private int horizonDays;
    private double probabilityPositiveReturn;
    private double expectedReturn;
    private double bullScore;
    private String model;
    private List<ExplanationItem> drivers;
    private String narrative;

    public MarketForecast() {
        this.drivers = Collections.emptyList();
    }

    public MarketForecast(String symbol, int horizonDays, double probabilityPositiveReturn, double expectedReturn,
                          double bullScore, String model, List<ExplanationItem> drivers, String narrative) {
        this.symbol = symbol;
        this.horizonDays = horizonDays;
        this.probabilityPositiveReturn = probabilityPositiveReturn;
        this.expectedReturn = expectedReturn;
        this.bullScore = bullScore;
        this.model = model;
        this.drivers = drivers == null ? Collections.emptyList() : List.copyOf(drivers);
        this.narrative = narrative;
    }

    public static MarketForecast unavailable(String symbol, int horizonDays, String reason) {
        return new MarketForecast(symbol, horizonDays, 0.5, 0.0, 50.0, "unavailable",
                List.of(ExplanationItem.text("forecast_unavailable", reason)),
                "Forecast is temporarily unavailable while model services warm up.");
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getHorizonDays() {
        return horizonDays;
    }

    public void setHorizonDays(int horizonDays) {
        this.horizonDays = horizonDays;
    }

    public double getProbabilityPositiveReturn() {
        return probabilityPositiveReturn;
    }

    public void setProbabilityPositiveReturn(double probabilityPositiveReturn) {
        this.probabilityPositiveReturn = probabilityPositiveReturn;
    }

    public double getExpectedReturn() {
        return expectedReturn;
    }

    public void setExpectedReturn(double expectedReturn) {
        this.expectedReturn = expectedReturn;
    }

    public double getBullScore() {
        return bullScore;
    }

    public void setBullScore(double bullScore) {
        this.bullScore = bullScore;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ExplanationItem> getDrivers() {
        return drivers;
    }

    public void setDrivers(List<ExplanationItem> drivers) {
        this.drivers = drivers == null ? Collections.emptyList() : List.copyOf(drivers);
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }
}
