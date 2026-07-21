package com.tradernet.jpa.dao;

import java.time.Instant;

/**
 * Persistence operations for closed market bars.
 */
public interface MarketBarDao {

    /**
     * Inserts a closed market bar unless the symbol/bucket key already exists.
     *
     * @return {@code true} when inserted, or {@code false} for a duplicate bar
     */
    boolean insert(
        String symbol,
        Instant bucket,
        double open,
        double high,
        double low,
        double close,
        double volume,
        String source
    );
}
