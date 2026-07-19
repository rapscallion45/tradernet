package com.tradernet.marketai.context;

import com.tradernet.marketai.model.ExplanationItem;

import java.util.Collections;
import java.util.List;

/**
 * Bounded market regime score used by signal scorers and APIs.
 */
public class MarketRegimeScore {

    private final int value;
    private final String regime;
    private final List<ExplanationItem> drivers;

    public MarketRegimeScore(int value, String regime, List<ExplanationItem> drivers) {
        this.value = value;
        this.regime = regime;
        this.drivers = drivers == null ? Collections.emptyList() : List.copyOf(drivers);
    }

    public int getValue() {
        return value;
    }

    public String getRegime() {
        return regime;
    }

    public List<ExplanationItem> getDrivers() {
        return drivers;
    }
}
