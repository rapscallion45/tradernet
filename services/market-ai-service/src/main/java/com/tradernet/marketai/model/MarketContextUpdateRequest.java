package com.tradernet.marketai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mutable market-context inputs accepted by the manual update API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketContextUpdateRequest {

    private Double etfFlowZScore;
    private Double exchangeOutflowZScore;
    private Double fundingRateZScore;
    private Double openInterestChangeZScore;
    private Double mvrvZScore;
    private Double liquidityGrowthZScore;
    private Double sentimentZScore;

    public Double getEtfFlowZScore() {
        return etfFlowZScore;
    }

    public void setEtfFlowZScore(Double etfFlowZScore) {
        this.etfFlowZScore = etfFlowZScore;
    }

    public Double getExchangeOutflowZScore() {
        return exchangeOutflowZScore;
    }

    public void setExchangeOutflowZScore(Double exchangeOutflowZScore) {
        this.exchangeOutflowZScore = exchangeOutflowZScore;
    }

    public Double getFundingRateZScore() {
        return fundingRateZScore;
    }

    public void setFundingRateZScore(Double fundingRateZScore) {
        this.fundingRateZScore = fundingRateZScore;
    }

    public Double getOpenInterestChangeZScore() {
        return openInterestChangeZScore;
    }

    public void setOpenInterestChangeZScore(Double openInterestChangeZScore) {
        this.openInterestChangeZScore = openInterestChangeZScore;
    }

    public Double getMvrvZScore() {
        return mvrvZScore;
    }

    public void setMvrvZScore(Double mvrvZScore) {
        this.mvrvZScore = mvrvZScore;
    }

    public Double getLiquidityGrowthZScore() {
        return liquidityGrowthZScore;
    }

    public void setLiquidityGrowthZScore(Double liquidityGrowthZScore) {
        this.liquidityGrowthZScore = liquidityGrowthZScore;
    }

    public Double getSentimentZScore() {
        return sentimentZScore;
    }

    public void setSentimentZScore(Double sentimentZScore) {
        this.sentimentZScore = sentimentZScore;
    }

    public boolean hasAnyUpdate() {
        return etfFlowZScore != null
            || exchangeOutflowZScore != null
            || fundingRateZScore != null
            || openInterestChangeZScore != null
            || mvrvZScore != null
            || liquidityGrowthZScore != null
            || sentimentZScore != null;
    }

    public MarketContextSnapshot toSnapshot(MarketContextSnapshot current) {
        final MarketContextSnapshot base = current == null ? MarketContextSnapshot.neutral() : current;
        return new MarketContextSnapshot(
            valueOrCurrent(etfFlowZScore, base.getEtfFlowZScore()),
            valueOrCurrent(exchangeOutflowZScore, base.getExchangeOutflowZScore()),
            valueOrCurrent(fundingRateZScore, base.getFundingRateZScore()),
            valueOrCurrent(openInterestChangeZScore, base.getOpenInterestChangeZScore()),
            valueOrCurrent(mvrvZScore, base.getMvrvZScore()),
            valueOrCurrent(liquidityGrowthZScore, base.getLiquidityGrowthZScore()),
            valueOrCurrent(sentimentZScore, base.getSentimentZScore()),
            true
        );
    }

    private double valueOrCurrent(Double updateValue, double currentValue) {
        return updateValue == null ? currentValue : updateValue;
    }
}
