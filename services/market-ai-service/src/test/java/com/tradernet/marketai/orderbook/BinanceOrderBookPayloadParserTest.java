package com.tradernet.marketai.orderbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BinanceOrderBookPayloadParserTest {

    private final BinanceOrderBookPayloadParser parser = new BinanceOrderBookPayloadParser(new ObjectMapper());

    @Test
    void parsesCompleteDepthUpdatesWithoutLosingLevelPrecision() {
        final BinanceOrderBookPayloadParser.DepthUpdate update = parser.parseDepthUpdate(
            "{\"E\":1234,\"U\":10,\"u\":12,\"pu\":9,"
                + "\"b\":[[\"64858.81000000\",\"0.00023000\"]],"
                + "\"a\":[[\"64860.55000000\",\"0.00000000\"]]}"
        );

        assertEquals(10L, update.firstUpdateId);
        assertEquals(12L, update.finalUpdateId);
        assertEquals(new BigDecimal("64858.81000000"), update.bids.get(0).price);
        assertEquals(new BigDecimal("0.00023000"), update.bids.get(0).quantity);
        assertEquals(BigDecimal.ZERO.setScale(8), update.asks.get(0).quantity);
    }

    @Test
    void rejectsTruncatedDepthMessages() {
        assertThrows(
            IllegalArgumentException.class,
            () -> parser.parseDepthUpdate("{\"U\":10,\"u\":12,\"b\":[[\"64858.81\",\"0.2\"]]")
        );
    }

    @Test
    void parsesRestSnapshotsAndOrdersBidLevelsDescending() {
        final BinanceOrderBookPayloadParser.SnapshotData snapshot = parser.parseSnapshot(
            "{\"lastUpdateId\":42,\"bids\":[[\"100\",\"1\"],[\"101\",\"2\"]],"
                + "\"asks\":[[\"102\",\"3\"]]}",
            5000L
        );

        assertEquals(42L, snapshot.lastUpdateId);
        assertEquals(new BigDecimal("101"), snapshot.bids.firstKey());
        assertEquals(new BigDecimal("102"), snapshot.asks.firstKey());
        assertEquals(5000L, snapshot.receivedAtMs);
    }
}
