package com.tradernet.marketai.context;

import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.MarketContextSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces a 0-100 market regime score from technical, on-chain, ETF, derivatives, macro, and sentiment features.
 */
public class MarketRegimeScoreEngine {

    public MarketRegimeScore score(FeatureSnapshot features) {
        final MarketContextSnapshot context = features.getMarketContext();
        final List<String> drivers = new ArrayList<>();
        double score = 50.0;

        final double trendScore = clamp((features.getEmaFast() - features.getEmaSlow()) / Math.max(features.getClose(), 1.0) * 1_000.0, -2.0, 2.0);
        final double rsiScore = rsiContribution(features.getRsi());
        final double etfScore = clamp(context.getEtfFlowZScore(), -2.0, 2.0);
        final double onChainScore = clamp(context.getExchangeOutflowZScore(), -2.0, 2.0);
        final double derivativesScore = derivativesContribution(context);
        final double valuationScore = valuationContribution(context.getMvrvZScore());
        final double liquidityScore = clamp(context.getLiquidityGrowthZScore(), -2.0, 2.0);
        final double sentimentScore = sentimentContribution(context.getSentimentZScore());

        score += trendScore * 6.0;
        score += rsiScore * 4.0;
        score += etfScore * 8.0;
        score += onChainScore * 7.0;
        score += derivativesScore * 6.0;
        score += valuationScore * 6.0;
        score += liquidityScore * 7.0;
        score += sentimentScore * 4.0;

        addDriver(drivers, "trend", trendScore);
        addDriver(drivers, "rsi", rsiScore);
        addDriver(drivers, "etf_flows", etfScore);
        addDriver(drivers, "exchange_outflows", onChainScore);
        addDriver(drivers, "derivatives", derivativesScore);
        addDriver(drivers, "mvrv_valuation", valuationScore);
        addDriver(drivers, "macro_liquidity", liquidityScore);
        addDriver(drivers, "sentiment", sentimentScore);

        final int boundedScore = (int) Math.round(clamp(score, 0.0, 100.0));
        return new MarketRegimeScore(boundedScore, regimeFor(boundedScore), drivers);
    }

    private double derivativesContribution(MarketContextSnapshot context) {
        final double openInterest = clamp(context.getOpenInterestChangeZScore(), -2.0, 2.0);
        final double funding = context.getFundingRateZScore();
        if (funding > 2.0) {
            return -2.0;
        }
        if (funding < -2.0) {
            return 1.0;
        }
        return clamp(openInterest - Math.max(funding - 1.0, 0.0), -2.0, 2.0);
    }

    private double valuationContribution(double mvrvZScore) {
        if (mvrvZScore >= 2.5) {
            return -2.0;
        }
        if (mvrvZScore >= 1.5) {
            return -1.0;
        }
        if (mvrvZScore <= -1.0) {
            return 1.5;
        }
        return 0.75;
    }

    private double sentimentContribution(double sentimentZScore) {
        if (sentimentZScore >= 2.0) {
            return -1.0;
        }
        if (sentimentZScore <= -2.0) {
            return 1.0;
        }
        return clamp(sentimentZScore, -1.0, 1.0);
    }

    private double rsiContribution(double rsi) {
        if (rsi >= 78.0) {
            return -2.0;
        }
        if (rsi >= 68.0) {
            return -1.0;
        }
        if (rsi <= 22.0) {
            return 1.0;
        }
        if (rsi <= 32.0) {
            return 0.5;
        }
        return 0.0;
    }

    private void addDriver(List<String> drivers, String name, double value) {
        if (Math.abs(value) >= 0.25) {
            drivers.add(name + "=" + String.format("%.2f", value));
        }
    }

    private String regimeFor(int score) {
        if (score < 25) {
            return "bearish";
        }
        if (score < 50) {
            return "weak";
        }
        if (score < 75) {
            return "bullish";
        }
        return "overheated";
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
