package com.tradernet.marketai.scoring;

/**
 * Creates scorer implementation based on runtime configuration. Defaults to context-aware scoring.
 */
public final class SignalScorerFactory {

    private SignalScorerFactory() {
    }

    public static SignalScorer create() {
        final String scorerType = System.getProperty("market.ai.scorer", "context").trim().toLowerCase();
        if ("rules".equals(scorerType)) {
            return new RuleBasedSignalScorer();
        }
        if ("linear".equals(scorerType)) {
            return new LinearModelSignalScorer();
        }
        return new ContextAwareSignalScorer();
    }
}
