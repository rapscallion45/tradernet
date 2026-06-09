package com.tradernet.marketai.scoring;

import com.tradernet.marketai.context.MarketRegimeScore;
import com.tradernet.marketai.context.MarketRegimeScoreEngine;
import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.SignalSide;

import java.util.ArrayList;
import java.util.List;

/**
 * Blends the short-term technical model with a broader symbol-specific market intelligence score.
 */
public class ContextAwareSignalScorer implements SignalScorer {

    private static final int BUY_SCORE_THRESHOLD = 58;
    private static final int SELL_SCORE_THRESHOLD = 42;

    private final SignalScorer technicalScorer;
    private final MarketRegimeScoreEngine regimeScoreEngine;

    public ContextAwareSignalScorer() {
        this(new LinearModelSignalScorer(), new MarketRegimeScoreEngine());
    }

    public ContextAwareSignalScorer(SignalScorer technicalScorer, MarketRegimeScoreEngine regimeScoreEngine) {
        this.technicalScorer = technicalScorer;
        this.regimeScoreEngine = regimeScoreEngine;
    }

    @Override
    public ScoreResult score(FeatureSnapshot features) {
        final ScoreResult technical = technicalScorer.score(features);
        final MarketRegimeScore regimeScore = regimeScoreEngine.score(features);
        final List<String> notes = new ArrayList<>(technical.getNotes());
        final boolean contextAvailable = features.getMarketContext().isAvailable();
        notes.add("market_score=" + regimeScore.getValue());
        notes.add("market_regime=" + regimeScore.getRegime());
        notes.addAll(regimeScore.getDrivers());

        if (technical.getSide() == SignalSide.BUY) {
            if (!contextAvailable) {
                notes.add("context_filter=unavailable_passthrough");
                return passThrough(technical, notes);
            }
            if (regimeScore.getValue() <= SELL_SCORE_THRESHOLD) {
                notes.add("context_filter=blocked_bearish_context");
                return hold(technical, regimeScore, notes);
            }
            notes.add(regimeScore.getValue() >= BUY_SCORE_THRESHOLD ? "context_filter=confirmed" : "context_filter=non_contradictory");
            return new ScoreResult(SignalSide.BUY, contextualConfidence(technical, regimeScore.getValue()), "context-v1", notes);
        }

        if (technical.getSide() == SignalSide.SELL) {
            if (!contextAvailable) {
                notes.add("context_filter=unavailable_passthrough");
                return passThrough(technical, notes);
            }
            if (regimeScore.getValue() >= BUY_SCORE_THRESHOLD) {
                notes.add("context_filter=blocked_bullish_context");
                return hold(technical, regimeScore, notes);
            }
            notes.add(regimeScore.getValue() <= SELL_SCORE_THRESHOLD ? "context_filter=confirmed" : "context_filter=non_contradictory");
            return new ScoreResult(SignalSide.SELL, contextualConfidence(technical, 100 - regimeScore.getValue()), "context-v1", notes);
        }

        if (contextAvailable && regimeScore.getValue() >= 72) {
            return new ScoreResult(SignalSide.BUY, scoreConfidence(regimeScore.getValue()), "context-v1", notes);
        }

        if (contextAvailable && regimeScore.getValue() <= 28) {
            return new ScoreResult(SignalSide.SELL, scoreConfidence(100 - regimeScore.getValue()), "context-v1", notes);
        }

        notes.add(contextAvailable ? "context_filter=hold" : "context_filter=unavailable_hold");
        return hold(technical, regimeScore, notes);
    }

    private ScoreResult passThrough(ScoreResult technical, List<String> notes) {
        return new ScoreResult(technical.getSide(), technical.getConfidence(), "context-v1", notes);
    }

    private ScoreResult hold(ScoreResult technical, MarketRegimeScore regimeScore, List<String> notes) {
        return new ScoreResult(SignalSide.HOLD, Math.max(technical.getConfidence(), scoreConfidence(Math.abs(regimeScore.getValue() - 50) + 50)), "context-v1", notes);
    }

    private double contextualConfidence(ScoreResult technical, int directionalScore) {
        if (directionalScore >= 58) {
            return blendConfidence(technical.getConfidence(), directionalScore);
        }
        return Math.max(0.60, technical.getConfidence() * 0.95);
    }

    private double blendConfidence(double technicalConfidence, int directionalScore) {
        return Math.min(0.97, (technicalConfidence * 0.60) + (scoreConfidence(directionalScore) * 0.40));
    }

    private double scoreConfidence(int directionalScore) {
        return Math.min(0.97, 0.50 + (Math.max(0, directionalScore - 50) / 100.0));
    }
}
