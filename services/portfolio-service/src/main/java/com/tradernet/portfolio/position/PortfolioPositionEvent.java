package com.tradernet.portfolio.position;

import java.time.Instant;

public final class PortfolioPositionEvent {

    private final String symbol;
    private final PortfolioPositionSide side;
    private final double quantityDelta;
    private final double price;
    private final Instant timestamp;

    public PortfolioPositionEvent(
        String symbol,
        PortfolioPositionSide side,
        double quantityDelta,
        double price,
        Instant timestamp
    ) {
        this.symbol = symbol;
        this.side = side;
        this.quantityDelta = quantityDelta;
        this.price = price;
        this.timestamp = timestamp;
    }

    public String getSymbol() { return symbol; }
    public PortfolioPositionSide getSide() { return side; }
    public double getQuantityDelta() { return quantityDelta; }
    public double getPrice() { return price; }
    public Instant getTimestamp() { return timestamp; }
}
