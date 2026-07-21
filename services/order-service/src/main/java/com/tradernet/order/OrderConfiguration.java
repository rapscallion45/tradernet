package com.tradernet.order;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Validated order workflow configuration.
 */
@Singleton
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OrderConfiguration {

    private int bullScoreHorizonDays;

    @PostConstruct
    void load() {
        final String configured = System.getProperty("market.ai.orderBullScoreHorizonDays", "1");
        try {
            bullScoreHorizonDays = Math.max(1, Math.min(365, Integer.parseInt(configured)));
        } catch (NumberFormatException ex) {
            bullScoreHorizonDays = 1;
        }
    }

    public int getBullScoreHorizonDays() {
        return bullScoreHorizonDays;
    }
}
