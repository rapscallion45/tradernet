package com.tradernet.marketai.scoring;

import com.tradernet.marketai.context.MarketRegimeScore;
import com.tradernet.marketai.context.MarketRegimeScoreEngine;
import com.tradernet.marketai.model.ExplanationItem;
import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.SignalSide;

import java.util.ArrayList;
import java.util.List;

/**
 * Blends the short-term technical model with broader symbol-specific context and forecast bull score.
 */
public class ContextAwareSignalScorer implements SignalScorer {

    private final SignalScorer technicalScorer;
    private final MarketRegimeScoreEngine regimeScoreEngine;
    private final int buyScoreThreshold;
    private final int sellScoreThreshold;
    private final int buyExtremeThreshold;
    private final int sellExtremeThreshold;
    private final double forecastWeight;
    private final double forecastNeutralBand;

    public ContextAwareSignalScorer() {
        this(SignalScoringSettings.defaults());
    }

    public ContextAwareSignalScorer(SignalScorer technicalScorer, MarketRegimeScoreEngine regimeScoreEngine) {
        this(technicalScorer, regimeScoreEngine, SignalScoringSettings.defaults());
    }

    public ContextAwareSignalScorer(SignalScoringSettings settings) {
        this(new LinearModelSignalScorer(settings), new MarketRegimeScoreEngine(), settings);
    }

    private ContextAwareSignalScorer(
        SignalScorer technicalScorer,
        MarketRegimeScoreEngine regimeScoreEngine,
        SignalScoringSettings settings
    ) {
        this.technicalScorer = technicalScorer;
        this.regimeScoreEngine = regimeScoreEngine;
        this.buyScoreThreshold = settings.getContextBuyScoreThreshold();
        this.sellScoreThreshold = settings.getContextSellScoreThreshold();
        this.buyExtremeThreshold = settings.getContextBuyExtremeThreshold();
        this.sellExtremeThreshold = settings.getContextSellExtremeThreshold();
        this.forecastWeight = settings.getForecastWeight();
        this.forecastNeutralBand = settings.getForecastNeutralBand();
    }

    @Override
    public ScoreResult score(FeatureSnapshot features) {
        final ScoreResult technical = technicalScorer.score(features);
        final MarketRegimeScore regimeScore = regimeScoreEngine.score(features);
        final List<ExplanationItem> notes = new ArrayList<>(technical.getNotes());
        final boolean contextAvailable = features.getMarketContext().isAvailable();
        final Double forecastBullScore = features.getForecastBullScore();
        final boolean forecastAvailable = forecastBullScore != null;
        final int effectiveScore = effectiveContextScore(regimeScore.getValue(), forecastBullScore, contextAvailable);
        final boolean directionalContextAvailable = contextAvailable || forecastAvailable;

        notes.add(ExplanationItem.numeric("market_score", "market_score", regimeScore.getValue()));
        notes.add(ExplanationItem.value("market_regime", "market_regime", regimeScore.getRegime()));
        if (forecastAvailable) {
            notes.add(ExplanationItem.numeric("forecast_bull_score", "forecast_bull_score", forecastBullScore));
            notes.add(ExplanationItem.numeric("effective_context_score", "effective_context_score", effectiveScore));
        }
        notes.addAll(regimeScore.getDrivers());

        if (technical.getSide() == SignalSide.BUY) {
            if (!directionalContextAvailable) {
                notes.add(ExplanationItem.value("context_filter", "context_filter", "unavailable_passthrough"));
                return passThrough(technical, notes);
            }
            if (forecastIsNeutral(forecastBullScore)) {
                notes.add(ExplanationItem.value("forecast_filter", "forecast_filter", "neutral_hold"));
                return hold(technical, effectiveScore, notes);
            }
            if (effectiveScore <= sellScoreThreshold) {
                notes.add(ExplanationItem.value("context_filter", "context_filter", "blocked_bearish_context"));
                return hold(technical, effectiveScore, notes);
            }
            notes.add(ExplanationItem.value("context_filter", "context_filter", effectiveScore >= buyScoreThreshold ? "confirmed" : "non_contradictory"));
            return new ScoreResult(SignalSide.BUY, contextualConfidence(technical, effectiveScore), "context-v2", notes);
        }

        if (technical.getSide() == SignalSide.SELL) {
            if (!directionalContextAvailable) {
                notes.add(ExplanationItem.value("context_filter", "context_filter", "unavailable_passthrough"));
                return passThrough(technical, notes);
            }
            if (forecastIsNeutral(forecastBullScore)) {
                notes.add(ExplanationItem.value("forecast_filter", "forecast_filter", "neutral_hold"));
                return hold(technical, effectiveScore, notes);
            }
            if (effectiveScore >= buyScoreThreshold) {
                notes.add(ExplanationItem.value("context_filter", "context_filter", "blocked_bullish_context"));
                return hold(technical, effectiveScore, notes);
            }
            notes.add(ExplanationItem.value("context_filter", "context_filter", effectiveScore <= sellScoreThreshold ? "confirmed" : "non_contradictory"));
            return new ScoreResult(SignalSide.SELL, contextualConfidence(technical, 100 - effectiveScore), "context-v2", notes);
        }

        if (directionalContextAvailable && effectiveScore >= buyExtremeThreshold) {
            notes.add(ExplanationItem.value("context_filter", "context_filter", "forecast_or_context_promoted_buy"));
            return new ScoreResult(SignalSide.BUY, scoreConfidence(effectiveScore), "context-v2", notes);
        }

        if (directionalContextAvailable && effectiveScore <= sellExtremeThreshold) {
            notes.add(ExplanationItem.value("context_filter", "context_filter", "forecast_or_context_promoted_sell"));
            return new ScoreResult(SignalSide.SELL, scoreConfidence(100 - effectiveScore), "context-v2", notes);
        }

        notes.add(ExplanationItem.value("context_filter", "context_filter", directionalContextAvailable ? "hold" : "unavailable_hold"));
        return hold(technical, effectiveScore, notes);
    }

    private int effectiveContextScore(int regimeScore, Double forecastBullScore, boolean contextAvailable) {
        final double baseScore = contextAvailable ? regimeScore : 50.0;
        if (forecastBullScore == null) {
            return (int) Math.round(baseScore);
        }
        final double boundedForecast = clamp(forecastBullScore, 0.0, 100.0);
        return (int) Math.round((baseScore * (1.0 - forecastWeight)) + (boundedForecast * forecastWeight));
    }

    private boolean forecastIsNeutral(Double forecastBullScore) {
        return forecastBullScore != null && Math.abs(clamp(forecastBullScore, 0.0, 100.0) - 50.0) <= forecastNeutralBand;
    }

    private ScoreResult passThrough(ScoreResult technical, List<ExplanationItem> notes) {
        return new ScoreResult(technical.getSide(), technical.getConfidence(), "context-v2", notes);
    }

    private ScoreResult hold(ScoreResult technical, int effectiveScore, List<ExplanationItem> notes) {
        final int distanceFromNeutral = Math.abs(effectiveScore - 50);
        final double contextHoldConfidence = Math.max(0.50, 0.98 - (distanceFromNeutral / 50.0));
        final double confidence = technical.getSide() == SignalSide.HOLD
                ? Math.max(technical.getConfidence(), contextHoldConfidence)
                : Math.min(technical.getConfidence(), contextHoldConfidence);
        return new ScoreResult(SignalSide.HOLD, clamp(confidence, 0.50, 0.98), "context-v2", notes);
    }

    private double contextualConfidence(ScoreResult technical, int directionalScore) {
        if (directionalScore >= buyScoreThreshold) {
            return blendConfidence(technical.getConfidence(), directionalScore);
        }
        return clamp(technical.getConfidence() * 0.95, 0.50, 0.98);
    }

    private double blendConfidence(double technicalConfidence, int directionalScore) {
        return clamp((technicalConfidence * 0.55) + (scoreConfidence(directionalScore) * 0.45), 0.50, 0.98);
    }

    private double scoreConfidence(int directionalScore) {
        return clamp(0.50 + (Math.max(0, directionalScore - 50) / 50.0), 0.50, 0.98);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
