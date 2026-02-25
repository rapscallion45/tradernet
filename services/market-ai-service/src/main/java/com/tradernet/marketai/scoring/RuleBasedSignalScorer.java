package com.tradernet.marketai.scoring;

import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.SignalSide;

import java.util.ArrayList;
import java.util.List;

/**
 * Existing threshold logic, preserved as a stable fallback scorer.
 */
public class RuleBasedSignalScorer implements SignalScorer {

    @Override
    public ScoreResult score(FeatureSnapshot features) {
        final List<String> notes = new ArrayList<>();
        SignalSide side = SignalSide.HOLD;
        double confidence = 0.5;

        if (features.getEmaFast() > features.getEmaSlow() && features.getRsi() < 65.0) {
            side = SignalSide.BUY;
            confidence = Math.min(0.95, 0.55 + (features.getEmaFast() - features.getEmaSlow()) / Math.max(features.getClose(), 1.0));
            notes.add("ema_bullish");
            notes.add("rsi_not_overbought");
        } else if (features.getEmaFast() < features.getEmaSlow() && features.getRsi() > 35.0) {
            side = SignalSide.SELL;
            confidence = Math.min(0.95, 0.55 + (features.getEmaSlow() - features.getEmaFast()) / Math.max(features.getClose(), 1.0));
            notes.add("ema_bearish");
            notes.add("rsi_not_oversold");
        }

        return new ScoreResult(side, confidence, "rules-v1", notes);
    }
}
