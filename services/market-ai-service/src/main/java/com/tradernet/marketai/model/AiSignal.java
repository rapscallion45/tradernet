package com.tradernet.marketai.model;

import java.util.Collections;
import java.util.List;

/**
 * AI-generated actionable signal event.
 */
public class AiSignal {

    private final String symbol;
    private final long eventTime;
    private final SignalSide side;
    private final double confidence;
    private final String modelVersion;
    private final List<String> notes;

    public AiSignal(String symbol, long eventTime, SignalSide side, double confidence, String modelVersion, List<String> notes) {
        this.symbol = symbol;
        this.eventTime = eventTime;
        this.side = side;
        this.confidence = confidence;
        this.modelVersion = modelVersion;
        this.notes = notes == null ? Collections.emptyList() : List.copyOf(notes);
    }

    public String getSymbol() {
        return symbol;
    }

    public long getEventTime() {
        return eventTime;
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
