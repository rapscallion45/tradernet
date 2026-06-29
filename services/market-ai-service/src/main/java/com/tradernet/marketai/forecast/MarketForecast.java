package com.tradernet.marketai.forecast;

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
    private List<String> drivers;
    private String narrative;

    public MarketForecast() {
        this.drivers = Collections.emptyList();
    }

    public MarketForecast(String symbol, int horizonDays, double probabilityPositiveReturn, double expectedReturn,
                          double bullScore, String model, List<String> drivers, String narrative) {
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
        return new MarketForecast(symbol, horizonDays, 0.5, 0.0, 50.0, "unavailable", List.of(reason),
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

    public List<String> getDrivers() {
        return drivers;
    }

    public void setDrivers(List<String> drivers) {
        this.drivers = drivers == null ? Collections.emptyList() : List.copyOf(drivers);
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public String getMarketConditionSummary() {
        final String condition = conditionLabel();
        final String probabilityText = probabilityLabel();
        final String expectedReturnText = expectedReturnLabel();
        return condition + " " + probabilityText + " " + expectedReturnText;
    }

    public void setMarketConditionSummary(String ignored) {
        // Derived read-only API field.
    }

    private String conditionLabel() {
        if (bullScore >= 75.0) {
            return "Conditions look strongly bullish for " + safeSymbol() + ".";
        }
        if (bullScore >= 60.0) {
            return "Conditions lean bullish for " + safeSymbol() + ".";
        }
        if (bullScore > 45.0 && bullScore < 55.0) {
            return "Conditions look balanced for " + safeSymbol() + ".";
        }
        if (bullScore >= 40.0) {
            return "Conditions look slightly cautious for " + safeSymbol() + ".";
        }
        return "Conditions look bearish for " + safeSymbol() + ".";
    }

    private String probabilityLabel() {
        final int probabilityPercent = (int) Math.round(clamp(probabilityPositiveReturn, 0.0, 1.0) * 100.0);
        if (probabilityPercent >= 65) {
            return "The " + horizonDays + "-day positive-return odds are elevated at " + probabilityPercent + "%.";
        }
        if (probabilityPercent <= 45) {
            return "The " + horizonDays + "-day positive-return odds are weak at " + probabilityPercent + "%.";
        }
        return "The " + horizonDays + "-day positive-return odds are mixed at " + probabilityPercent + "%.";
    }

    private String expectedReturnLabel() {
        final double expectedReturnPercent = expectedReturn * 100.0;
        if (expectedReturnPercent >= 1.0) {
            return "The model expects a modest positive move.";
        }
        if (expectedReturnPercent <= -1.0) {
            return "The model expects a modest negative move.";
        }
        return "The model expects a mostly flat move.";
    }

    private String safeSymbol() {
        return symbol == null || symbol.isBlank() ? "the selected market" : symbol.trim().toUpperCase();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
