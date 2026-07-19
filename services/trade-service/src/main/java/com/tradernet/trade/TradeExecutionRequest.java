package com.tradernet.trade;

/**
 * Service-layer command describing a trade fill to persist.
 */
public class TradeExecutionRequest {

    private final Long userId;
    private final Long orderId;
    private final String symbol;
    private final TradeSide side;
    private final TradeExecutionType executionType;
    private final double quantity;
    private final double price;

    public TradeExecutionRequest(
        Long userId,
        Long orderId,
        String symbol,
        TradeSide side,
        TradeExecutionType executionType,
        double quantity,
        double price
    ) {
        this.userId = userId;
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.executionType = executionType;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public TradeSide getSide() {
        return side;
    }

    public TradeExecutionType getExecutionType() {
        return executionType;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }
}
