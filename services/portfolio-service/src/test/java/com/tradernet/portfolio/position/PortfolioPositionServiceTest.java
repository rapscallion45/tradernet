package com.tradernet.portfolio.position;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioPositionServiceTest {

    private final PortfolioPositionService service = new PortfolioPositionService();

    @Test
    void preservesSignedLongAndShortPositions() {
        final Map<String, PortfolioPosition> positions = service.aggregate(List.of(
            event("BTCUSDT", PortfolioPositionSide.BUY, 2.0, 100.0),
            event("ETHUSDT", PortfolioPositionSide.SELL, -3.0, 50.0)
        ));

        assertEquals(2.0, positions.get("BTCUSDT").getNetQuantity());
        assertEquals(100.0, positions.get("BTCUSDT").getAverageCost());
        assertEquals(-3.0, positions.get("ETHUSDT").getNetQuantity());
        assertEquals(50.0, positions.get("ETHUSDT").getAverageCost());
    }

    @Test
    void closesAndReversesPositionsWithoutCarryingTheOldCostBasis() {
        final Map<String, PortfolioPosition> positions = service.aggregate(List.of(
            event("BTCUSDT", PortfolioPositionSide.SELL, -6.0, 100.0),
            event("BTCUSDT", PortfolioPositionSide.BUY, 8.0, 90.0),
            event("ETHUSDT", PortfolioPositionSide.BUY, 2.0, 50.0),
            event("ETHUSDT", PortfolioPositionSide.SELL, -2.0, 60.0)
        ));

        assertEquals(2.0, positions.get("BTCUSDT").getNetQuantity());
        assertEquals(90.0, positions.get("BTCUSDT").getAverageCost());
        assertTrue(positions.get("ETHUSDT").isFlat());
    }

    private PortfolioPositionEvent event(
        String symbol,
        PortfolioPositionSide side,
        double quantityDelta,
        double price
    ) {
        return new PortfolioPositionEvent(symbol, side, quantityDelta, price, Instant.EPOCH);
    }
}
