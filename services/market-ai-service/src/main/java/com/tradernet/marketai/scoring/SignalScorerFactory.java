package com.tradernet.marketai.scoring;

/**
 * Creates scorer implementation based on runtime configuration.
 */
public final class SignalScorerFactory {

    private SignalScorerFactory() {
    }

    public static SignalScorer create() {
        final String scorerType = System.getProperty("market.ai.scorer", "linear").trim().toLowerCase();
        if ("rules".equals(scorerType)) {
            return new RuleBasedSignalScorer();
        }
        return new LinearModelSignalScorer();
    }
}
