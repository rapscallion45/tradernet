package com.tradernet.marketai.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartIntervalTest {

    @Test
    void parsesAdvertisedMinuteAlias() {
        final ChartInterval interval = ChartInterval.parse("15MIN");

        assertEquals("15MIN", interval.getToken());
        assertEquals(15L * 60_000L, interval.getDurationMs());
    }

    @Test
    void fallsBackForUnsupportedUnits() {
        final ChartInterval interval = ChartInterval.parse("1WEEK");

        assertEquals("1S", interval.getToken());
        assertEquals(1_000L, interval.getDurationMs());
    }
}
