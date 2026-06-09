package com.tradernet.marketai.context;

import java.util.Collections;
import java.util.List;

/**
 * Bounded market regime score used by signal scorers and APIs.
 */
public class MarketRegimeScore {

    private final int value;
    private final String regime;
    private final List<String> drivers;

    public MarketRegimeScore(int value, String regime, List<String> drivers) {
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

    public List<String> getDrivers() {
        return drivers;
    }
}
