package com.tradernet.marketai.scoring;

/**
 * Creates scorer implementation based on runtime configuration. Defaults to context-aware scoring.
 */
public final class SignalScorerFactory {

    private SignalScorerFactory() {
    }

    public static SignalScorer create() {
        return create(SignalScoringSettings.defaults());
    }

    public static SignalScorer create(SignalScoringSettings settings) {
        final String scorerType = settings.getScorerType();
        if ("rules".equals(scorerType)) {
            return new RuleBasedSignalScorer();
        }
        if ("linear".equals(scorerType)) {
            return new LinearModelSignalScorer(settings);
        }
        return new ContextAwareSignalScorer(settings);
    }
}
