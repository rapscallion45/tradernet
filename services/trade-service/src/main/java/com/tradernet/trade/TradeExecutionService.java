package com.tradernet.trade;

import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.jpa.entities.TradeEntity;

/**
 * Executes orders and returns a Trade.
 * In a real system this would integrate with a broker/exchange.
 */
public class TradeExecutionService {

    /**
     * Executes the given order.
     *
     * @param order order to execute
     * @return Trade object representing the completed trade
     */
    public TradeEntity execute(OrderEntity order) {
        // Stub execution; in real life this hits an exchange
        return new TradeEntity(order.getSymbol(), order.getQuantity(), order.getPrice());
    }
}
