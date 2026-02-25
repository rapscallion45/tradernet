package com.tradernet.marketai.scoring;

import com.tradernet.marketai.model.SignalSide;

import java.util.Collections;
import java.util.List;

/**
 * Immutable output of a scorer before engine-level filtering/cooldown is applied.
 */
public class ScoreResult {

    private final SignalSide side;
    private final double confidence;
    private final String modelVersion;
    private final List<String> notes;

    public ScoreResult(SignalSide side, double confidence, String modelVersion, List<String> notes) {
        this.side = side;
        this.confidence = confidence;
        this.modelVersion = modelVersion;
        this.notes = notes == null ? Collections.emptyList() : List.copyOf(notes);
    }

    public SignalSide getSide() {
        return side;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public List<String> getNotes() {
        return notes;
    }
}
