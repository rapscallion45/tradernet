package com.tradernet.marketai.engine;

import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.SignalSide;
import com.tradernet.marketai.scoring.ScoreResult;
import com.tradernet.marketai.scoring.SignalScorer;
import com.tradernet.marketai.scoring.SignalScorerFactory;

/**
 * Signal engine that applies cooldown/threshold guardrails around a pluggable scorer.
 */
public class AiSignalEngine {

    private static final double MIN_CONFIDENCE = 0.60;
    private static final long COOLDOWN_MS = 10_000L;

    private final SignalScorer scorer;
    private long lastSignalAt;

    public AiSignalEngine() {
        this(SignalScorerFactory.create());
    }

    public AiSignalEngine(SignalScorer scorer) {
        this.scorer = scorer;
    }

    public synchronized AiSignal evaluate(FeatureSnapshot features) {
        final long now = features.getEventTime();
        if (now - lastSignalAt < COOLDOWN_MS) {
            return null;
        }

        final ScoreResult scored = scorer.score(features);
        if (scored.getSide() == SignalSide.HOLD || scored.getConfidence() < MIN_CONFIDENCE) {
            return null;
        }

        lastSignalAt = now;
        return new AiSignal(features.getSymbol(), features.getEventTime(), scored.getSide(), scored.getConfidence(), scored.getModelVersion(), scored.getNotes());
    }
}
