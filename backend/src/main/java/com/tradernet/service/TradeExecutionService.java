package com.tradernet.service;

import com.tradernet.model.Order;
import com.tradernet.model.Trade;

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
    public Trade execute(Order order) {
        // Stub execution; in real life this hits an exchange
        return new Trade(order.getSymbol(), order.getQuantity(), order.getPrice());
    }
}
