package com.tradernet.marketai.scoring;

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

    private static final double DEFAULT_BUY_THRESHOLD = 0.62;
    private static final double DEFAULT_SELL_THRESHOLD = 0.38;

    private final double buyThreshold;
    private final double sellThreshold;

    public LinearModelSignalScorer() {
        this.buyThreshold = Double.parseDouble(System.getProperty("market.ai.model.buyThreshold", String.valueOf(DEFAULT_BUY_THRESHOLD)));
        this.sellThreshold = Double.parseDouble(System.getProperty("market.ai.model.sellThreshold", String.valueOf(DEFAULT_SELL_THRESHOLD)));
    }

    @Override
    public ScoreResult score(FeatureSnapshot features) {
        final double emaDeltaPct = (features.getEmaFast() - features.getEmaSlow()) / Math.max(features.getClose(), 1.0);
        final double rsiCentered = (features.getRsi() - 50.0) / 50.0;

        // Logistic score in [0,1]. Positive values favor BUY, negative values favor SELL.
        final double linear = (emaDeltaPct * 120.0) + (rsiCentered * 0.9);
        final double probabilityBuy = 1.0 / (1.0 + Math.exp(-linear));

        final List<String> notes = new ArrayList<>();
        notes.add("model=linear-logit");
        notes.add("ema_delta_pct=" + String.format("%.6f", emaDeltaPct));
        notes.add("rsi=" + String.format("%.2f", features.getRsi()));

        if (probabilityBuy >= buyThreshold) {
            return new ScoreResult(SignalSide.BUY, probabilityBuy, "linear-v1", notes);
        }

        if (probabilityBuy <= sellThreshold) {
            return new ScoreResult(SignalSide.SELL, 1.0 - probabilityBuy, "linear-v1", notes);
        }

        return new ScoreResult(SignalSide.HOLD, Math.max(probabilityBuy, 1.0 - probabilityBuy), "linear-v1", notes);
    }
}
