package com.tradernet.marketai.orderbook;

/**
 * Runtime state for a locally maintained exchange order book.
 */
public enum OrderBookStatus {
    LIVE,
    SYNCING,
    SNAPSHOT_ONLY,
    STALE,
    UNAVAILABLE
}
