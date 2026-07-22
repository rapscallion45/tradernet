package com.tradernet.marketai;

/**
 * Raised when a bounded live market client pool has no available capacity.
 */
public class MarketDataCapacityException extends RuntimeException {

    public MarketDataCapacityException(String message) {
        super(message);
    }
}
