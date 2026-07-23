package com.tradernet.marketai.scoring;

/**
 * Immutable configuration consumed by per-symbol scoring engines.
 */
public final class SignalScoringSettings {

    private final String scorerType;
    private final double modelBuyThreshold;
    private final double modelSellThreshold;
    private final int contextBuyScoreThreshold;
    private final int contextSellScoreThreshold;
    private final int contextBuyExtremeThreshold;
    private final int contextSellExtremeThreshold;
    private final double forecastWeight;
    private final double forecastNeutralBand;

    public SignalScoringSettings(
        String scorerType,
        double modelBuyThreshold,
        double modelSellThreshold,
        int contextBuyScoreThreshold,
        int contextSellScoreThreshold,
        int contextBuyExtremeThreshold,
        int contextSellExtremeThreshold,
        double forecastWeight,
        double forecastNeutralBand
    ) {
        this.scorerType = scorerType;
        this.modelBuyThreshold = modelBuyThreshold;
        this.modelSellThreshold = modelSellThreshold;
        this.contextBuyScoreThreshold = contextBuyScoreThreshold;
        this.contextSellScoreThreshold = contextSellScoreThreshold;
        this.contextBuyExtremeThreshold = contextBuyExtremeThreshold;
        this.contextSellExtremeThreshold = contextSellExtremeThreshold;
        this.forecastWeight = forecastWeight;
        this.forecastNeutralBand = forecastNeutralBand;
    }

    public static SignalScoringSettings defaults() {
        return new SignalScoringSettings("context", 0.56, 0.44, 54, 46, 64, 36, 0.45, 8.0);
    }

    public String getScorerType() {
        return scorerType;
    }

    public double getModelBuyThreshold() {
        return modelBuyThreshold;
    }

    public double getModelSellThreshold() {
        return modelSellThreshold;
    }

    public int getContextBuyScoreThreshold() {
        return contextBuyScoreThreshold;
    }

    public int getContextSellScoreThreshold() {
        return contextSellScoreThreshold;
    }

    public int getContextBuyExtremeThreshold() {
        return contextBuyExtremeThreshold;
    }

    public int getContextSellExtremeThreshold() {
        return contextSellExtremeThreshold;
    }

    public double getForecastWeight() {
        return forecastWeight;
    }

    public double getForecastNeutralBand() {
        return forecastNeutralBand;
    }
}
