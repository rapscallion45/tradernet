package com.tradernet.marketai.scoring;

import com.tradernet.marketai.model.FeatureSnapshot;

/**
 * Scorer contract so production can swap between rule-based and model-based strategies.
 */
public interface SignalScorer {

    ScoreResult score(FeatureSnapshot features);
}
