package com.tradernet.order;

import com.tradernet.order.dto.OrderSide;

/**
 * Service command for creating an order.
 */
public final class CreateOrderCommand {

    private final String symbol;
    private final double quantity;
    private final double price;
    private final OrderSide side;

    public CreateOrderCommand(String symbol, double quantity, double price, OrderSide side) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public OrderSide getSide() {
        return side;
    }
}
