package com.tradernet.marketai.model;

/**
 * Normalized market context used to improve buy/sell signal accuracy.
 *
 * <p>Values are expected to be z-scores or bounded directional scores where positive values are bullish and
 * negative values are bearish. The snapshot is intentionally provider-agnostic so ingestion jobs can hydrate it from
 * ETF flows, on-chain data, derivatives, macro liquidity, and sentiment sources.</p>
 */
public class MarketContextSnapshot {

    private double etfFlowZScore;
    private double exchangeOutflowZScore;
    private double fundingRateZScore;
    private double openInterestChangeZScore;
    private double mvrvZScore;
    private double liquidityGrowthZScore;
    private double sentimentZScore;
    private boolean available;

    public MarketContextSnapshot() {
        this(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false);
    }

    public MarketContextSnapshot(double etfFlowZScore,
                                 double exchangeOutflowZScore,
                                 double fundingRateZScore,
                                 double openInterestChangeZScore,
                                 double mvrvZScore,
                                 double liquidityGrowthZScore,
                                 double sentimentZScore) {
        this(etfFlowZScore, exchangeOutflowZScore, fundingRateZScore, openInterestChangeZScore, mvrvZScore, liquidityGrowthZScore, sentimentZScore, true);
    }

    public MarketContextSnapshot(double etfFlowZScore,
                                 double exchangeOutflowZScore,
                                 double fundingRateZScore,
                                 double openInterestChangeZScore,
                                 double mvrvZScore,
                                 double liquidityGrowthZScore,
                                 double sentimentZScore,
                                 boolean available) {
        this.etfFlowZScore = etfFlowZScore;
        this.exchangeOutflowZScore = exchangeOutflowZScore;
        this.fundingRateZScore = fundingRateZScore;
        this.openInterestChangeZScore = openInterestChangeZScore;
        this.mvrvZScore = mvrvZScore;
        this.liquidityGrowthZScore = liquidityGrowthZScore;
        this.sentimentZScore = sentimentZScore;
        this.available = available;
    }

    public static MarketContextSnapshot neutral() {
        return new MarketContextSnapshot(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false);
    }

    public double getEtfFlowZScore() {
        return etfFlowZScore;
    }

    public double getExchangeOutflowZScore() {
        return exchangeOutflowZScore;
    }

    public double getFundingRateZScore() {
        return fundingRateZScore;
    }

    public double getOpenInterestChangeZScore() {
        return openInterestChangeZScore;
    }

    public double getMvrvZScore() {
        return mvrvZScore;
    }

    public double getLiquidityGrowthZScore() {
        return liquidityGrowthZScore;
    }

    public double getSentimentZScore() {
        return sentimentZScore;
    }

    public boolean isAvailable() {
        return available;
    }

    public int getEtfFlowBullishPercent() {
        return bullishPercent(clamp(etfFlowZScore, -2.0, 2.0));
    }

    public int getExchangeOutflowBullishPercent() {
        return bullishPercent(clamp(exchangeOutflowZScore, -2.0, 2.0));
    }

    public int getFundingRateBullishPercent() {
        return bullishPercent(fundingSignalScore(fundingRateZScore));
    }

    public int getOpenInterestChangeBullishPercent() {
        return bullishPercent(clamp(openInterestChangeZScore, -2.0, 2.0));
    }

    public int getMvrvBullishPercent() {
        return bullishPercent(valuationSignalScore(mvrvZScore));
    }

    public int getLiquidityGrowthBullishPercent() {
        return bullishPercent(clamp(liquidityGrowthZScore, -2.0, 2.0));
    }

    public int getSentimentBullishPercent() {
        return bullishPercent(sentimentSignalScore(sentimentZScore));
    }

    public void setEtfFlowZScore(double etfFlowZScore) {
        this.etfFlowZScore = etfFlowZScore;
    }

    public void setExchangeOutflowZScore(double exchangeOutflowZScore) {
        this.exchangeOutflowZScore = exchangeOutflowZScore;
    }

    public void setFundingRateZScore(double fundingRateZScore) {
        this.fundingRateZScore = fundingRateZScore;
    }

    public void setOpenInterestChangeZScore(double openInterestChangeZScore) {
        this.openInterestChangeZScore = openInterestChangeZScore;
    }

    public void setMvrvZScore(double mvrvZScore) {
        this.mvrvZScore = mvrvZScore;
    }

    public void setLiquidityGrowthZScore(double liquidityGrowthZScore) {
        this.liquidityGrowthZScore = liquidityGrowthZScore;
    }

    public void setSentimentZScore(double sentimentZScore) {
        this.sentimentZScore = sentimentZScore;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setEtfFlowBullishPercent(int ignored) {
        // Derived read-only API field.
    }

    public void setExchangeOutflowBullishPercent(int ignored) {
        // Derived read-only API field.
    }

    public void setFundingRateBullishPercent(int ignored) {
        // Derived read-only API field.
    }

    public void setOpenInterestChangeBullishPercent(int ignored) {
        // Derived read-only API field.
    }

    public void setMvrvBullishPercent(int ignored) {
        // Derived read-only API field.
    }

    public void setLiquidityGrowthBullishPercent(int ignored) {
        // Derived read-only API field.
    }

    public void setSentimentBullishPercent(int ignored) {
        // Derived read-only API field.
    }

    private int bullishPercent(double signalScore) {
        return (int) Math.round(((clamp(signalScore, -2.0, 2.0) + 2.0) / 4.0) * 100.0);
    }

    private double fundingSignalScore(double fundingRate) {
        if (fundingRate > 2.0) {
            return -2.0;
        }
        if (fundingRate < -2.0) {
            return 1.0;
        }
        if (fundingRate > 1.0) {
            return clamp(-(fundingRate - 1.0), -2.0, 0.0);
        }
        if (fundingRate < -1.0) {
            return 0.5;
        }
        return 0.0;
    }

    private double valuationSignalScore(double mvrvScore) {
        if (mvrvScore >= 2.5) {
            return -2.0;
        }
        if (mvrvScore >= 1.5) {
            return -1.0;
        }
        if (mvrvScore <= -1.0) {
            return 1.5;
        }
        return 0.75;
    }

    private double sentimentSignalScore(double sentimentScore) {
        if (sentimentScore >= 2.0) {
            return -1.0;
        }
        if (sentimentScore <= -2.0) {
            return 1.0;
        }
        return clamp(sentimentScore, -1.0, 1.0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
