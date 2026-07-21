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

    public boolean hasOnlyFiniteValues() {
        return isFinite(etfFlowZScore)
            && isFinite(exchangeOutflowZScore)
            && isFinite(fundingRateZScore)
            && isFinite(openInterestChangeZScore)
            && isFinite(mvrvZScore)
            && isFinite(liquidityGrowthZScore)
            && isFinite(sentimentZScore);
    }

    public MarketContextSnapshot toSnapshot(MarketContextSnapshot current) {
        final MarketContextSnapshot next = current == null ? MarketContextSnapshot.neutral() : current.copy();
        if (etfFlowZScore != null) {
            next.setEtfFlowZScore(etfFlowZScore);
        }
        if (exchangeOutflowZScore != null) {
            next.setExchangeOutflowZScore(exchangeOutflowZScore);
        }
        if (fundingRateZScore != null) {
            next.setFundingRateZScore(fundingRateZScore);
        }
        if (openInterestChangeZScore != null) {
            next.setOpenInterestChangeZScore(openInterestChangeZScore);
        }
        if (mvrvZScore != null) {
            next.setMvrvZScore(mvrvZScore);
        }
        if (liquidityGrowthZScore != null) {
            next.setLiquidityGrowthZScore(liquidityGrowthZScore);
        }
        if (sentimentZScore != null) {
            next.setSentimentZScore(sentimentZScore);
        }
        next.setAvailable(true);
        return next;
    }

    private boolean isFinite(Double value) {
        return value == null || Double.isFinite(value);
    }
}
