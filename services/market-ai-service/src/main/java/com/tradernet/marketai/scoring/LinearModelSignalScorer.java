package com.tradernet.marketai.scoring;

import com.tradernet.marketai.model.ExplanationItem;
import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.SignalSide;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight model scorer that can run without external ML runtime dependencies.
 *
 * This is a production-friendly stepping stone before introducing ONNX/XGBoost runtime integration.
 */
public class LinearModelSignalScorer implements SignalScorer {

    private final double buyThreshold;
    private final double sellThreshold;

    public LinearModelSignalScorer() {
        this(SignalScoringSettings.defaults());
    }

    public LinearModelSignalScorer(SignalScoringSettings settings) {
        this.buyThreshold = settings.getModelBuyThreshold();
        this.sellThreshold = settings.getModelSellThreshold();
    }

    @Override
    public ScoreResult score(FeatureSnapshot features) {
        final double emaDeltaPct = (features.getEmaFast() - features.getEmaSlow()) / Math.max(features.getClose(), 1.0);
        final double rsiCentered = (features.getRsi() - 50.0) / 50.0;

        // Logistic score in [0,1]. Positive values favor BUY, negative values favor SELL.
        final double linear = (emaDeltaPct * 120.0) + (rsiCentered * 0.9);
        final double probabilityBuy = 1.0 / (1.0 + Math.exp(-linear));

        final List<ExplanationItem> notes = new ArrayList<>();
        notes.add(ExplanationItem.value("model", "model", "linear-logit"));
        notes.add(ExplanationItem.numeric("ema_delta_pct", "ema_delta_pct", emaDeltaPct));
        notes.add(ExplanationItem.numeric("rsi", "rsi", features.getRsi()));

        if (probabilityBuy >= buyThreshold) {
            return new ScoreResult(SignalSide.BUY, directionalConfidence(probabilityBuy, buyThreshold, 1.0), "linear-v1", notes);
        }

        if (probabilityBuy <= sellThreshold) {
            return new ScoreResult(SignalSide.SELL, directionalConfidence(1.0 - probabilityBuy, 1.0 - sellThreshold, 1.0), "linear-v1", notes);
        }

        return new ScoreResult(SignalSide.HOLD, holdConfidence(probabilityBuy), "linear-v1", notes);
    }

    private double directionalConfidence(double directionalProbability, double threshold, double upperBound) {
        final double normalized = (directionalProbability - threshold) / Math.max(upperBound - threshold, 0.0001);
        return clamp(0.60 + (normalized * 0.38), 0.60, 0.98);
    }

    private double holdConfidence(double probabilityBuy) {
        final double neutralDistance = Math.abs(probabilityBuy - 0.50);
        return clamp(0.98 - (neutralDistance * 2.0), 0.50, 0.98);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
