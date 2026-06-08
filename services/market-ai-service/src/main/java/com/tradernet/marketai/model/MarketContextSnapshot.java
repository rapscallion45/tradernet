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

    public MarketContextSnapshot() {
        this(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    public MarketContextSnapshot(double etfFlowZScore,
                                 double exchangeOutflowZScore,
                                 double fundingRateZScore,
                                 double openInterestChangeZScore,
                                 double mvrvZScore,
                                 double liquidityGrowthZScore,
                                 double sentimentZScore) {
        this.etfFlowZScore = etfFlowZScore;
        this.exchangeOutflowZScore = exchangeOutflowZScore;
        this.fundingRateZScore = fundingRateZScore;
        this.openInterestChangeZScore = openInterestChangeZScore;
        this.mvrvZScore = mvrvZScore;
        this.liquidityGrowthZScore = liquidityGrowthZScore;
        this.sentimentZScore = sentimentZScore;
    }

    public static MarketContextSnapshot neutral() {
        return new MarketContextSnapshot(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
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
}
