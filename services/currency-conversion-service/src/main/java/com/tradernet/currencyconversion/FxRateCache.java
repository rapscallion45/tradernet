package com.tradernet.currencyconversion;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores only provider-sourced FX rates and applies their freshness policy.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class FxRateCache {

    private static final long CURRENT_RATE_TTL_MS = Duration.ofHours(1).toMillis();

    private final Map<String, CachedRate> rates = new ConcurrentHashMap<>();

    public Optional<BigDecimal> find(CurrencyCode from, CurrencyCode to, LocalDate date) {
        final String key = key(from, to, date);
        final CachedRate cached = rates.get(key);
        if (cached == null) {
            return Optional.empty();
        }
        if (!cached.isFresh()) {
            rates.remove(key, cached);
            return Optional.empty();
        }
        return Optional.of(cached.rate);
    }

    public void put(CurrencyCode from, CurrencyCode to, LocalDate date, BigDecimal rate) {
        if (rate == null || rate.signum() <= 0) {
            return;
        }
        final boolean currentDate = date.equals(LocalDate.now(ZoneOffset.UTC));
        final long expiresAtMs = currentDate
            ? System.currentTimeMillis() + CURRENT_RATE_TTL_MS
            : Long.MAX_VALUE;
        rates.put(key(from, to, date), new CachedRate(rate, expiresAtMs));
    }

    private String key(CurrencyCode from, CurrencyCode to, LocalDate date) {
        return from.name() + '_' + to.name() + '_' + date;
    }

    private static final class CachedRate {
        private final BigDecimal rate;
        private final long expiresAtMs;

        private CachedRate(BigDecimal rate, long expiresAtMs) {
            this.rate = rate;
            this.expiresAtMs = expiresAtMs;
        }

        private boolean isFresh() {
            return expiresAtMs == Long.MAX_VALUE || System.currentTimeMillis() < expiresAtMs;
        }
    }
}
