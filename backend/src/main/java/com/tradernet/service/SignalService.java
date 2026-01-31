package com.tradernet.service;

import com.tradernet.core.TradingSignal;
import com.tradernet.model.Order;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for generating and validating trading signals.
 */
public class SignalService {

    // Stores all signals generated in memory
    private final List<TradingSignal> signals = new ArrayList<>();

    /**
     * Validates a trading signal according to custom rules.
     *
     * @param signal the trading signal to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidSignal(TradingSignal signal) {
        // Example validation: price must be positive
        return signal.getPrice() > 0;
    }

    /**
     * Adds a valid trading signal to the service.
     *
     * @param signal the trading signal to add
     */
    public void addSignal(TradingSignal signal) {
        if (!isValidSignal(signal)) {
            throw new IllegalArgumentException("Invalid trading signal: " + signal);
        }
        signals.add(signal);
    }

    /**
     * Checks if the given order is valid according to trading rules.
     *
     * @param order the order to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidOrder(Order order) {
        // Example simple validation
        if (order.getQuantity() <= 0) return false;

        // Compare with enum values instead of strings
        Order.Side side = order.getSide();
        if (side != Order.Side.BUY && side != Order.Side.SELL) return false;

        return order.getPrice() > 0;
    }

    /**
     * Returns a list of all generated signals.
     *
     * @return list of signals
     */
    public List<TradingSignal> getSignals() {
        return new ArrayList<>(signals); // return copy to prevent external mutation
    }
}
