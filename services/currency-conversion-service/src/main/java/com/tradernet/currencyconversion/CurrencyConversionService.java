package com.tradernet.currencyconversion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;

/**
 * Applies conversion policy over provider-backed, cached FX rates.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CurrencyConversionService implements CurrencyConversionProvider {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final int CURRENCY_SCALE = 2;

    @EJB
    private FxRateGateway rateGateway;

    @EJB
    private FxRateCache rateCache;

    public CurrencyConversionService() {
    }

    CurrencyConversionService(FxRateGateway rateGateway, FxRateCache rateCache) {
        this.rateGateway = rateGateway;
        this.rateCache = rateCache;
    }

    @Override
    public List<String> getSupportedCurrencies() {
        final List<String> supported = new ArrayList<>();
        for (CurrencyCode currencyCode : CurrencyCode.values()) {
            supported.add(currencyCode.name());
        }
        supported.sort(Comparator.naturalOrder());
        return supported;
    }

    @Override
    public CurrencyCode resolveQuoteCurrency(String symbol) {
        if (symbol == null) {
            return CurrencyCode.USD;
        }

        final String upper = symbol.trim().toUpperCase();
        if (upper.endsWith("USDT") || upper.endsWith("USD")) {
            return CurrencyCode.USD;
        }

        for (CurrencyCode currency : CurrencyCode.values()) {
            if (upper.endsWith(currency.name())) {
                return currency;
            }
        }
        return CurrencyCode.USD;
    }

    @Override
    public double convertAmount(double amount, CurrencyCode from, CurrencyCode to, Instant timestamp) {
        if (from == to) {
            return amount;
        }

        final BigDecimal rate = getRate(from, to, timestamp == null ? Instant.now() : timestamp);
        return BigDecimal.valueOf(amount)
            .multiply(rate, MC)
            .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)
            .doubleValue();
    }

    @Override
    public BigDecimal getRate(CurrencyCode from, CurrencyCode to, Instant timestamp) {
        Objects.requireNonNull(from, "source currency is required");
        Objects.requireNonNull(to, "target currency is required");
        if (from == to) {
            return BigDecimal.ONE;
        }

        final LocalDate date = LocalDate.ofInstant(timestamp == null ? Instant.now() : timestamp, ZoneOffset.UTC);
        return rateCache.find(from, to, date).orElseGet(() -> {
            final BigDecimal rate = rateGateway.findRate(from, to, date)
                .orElseThrow(() -> unavailable(from, to, date));
            rateCache.put(from, to, date, rate);
            return rate;
        });
    }

    @Override
    public void prefetchRates(CurrencyCode from, CurrencyCode to, Instant start, Instant end) {
        if (from == to || start == null || end == null) {
            return;
        }

        final LocalDate startDate = LocalDate.ofInstant(start, ZoneOffset.UTC);
        final LocalDate endDate = LocalDate.ofInstant(end, ZoneOffset.UTC);
        if (startDate.isAfter(endDate) || isRangeCached(from, to, startDate, endDate)) {
            return;
        }

        final NavigableMap<LocalDate, BigDecimal> providerRates = rateGateway.findRates(
            from,
            to,
            startDate.minusDays(7),
            endDate
        );
        if (providerRates.isEmpty()) {
            throw unavailable(from, to, endDate);
        }

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final Map.Entry<LocalDate, BigDecimal> providerRate = providerRates.floorEntry(date);
            if (providerRate == null) {
                throw unavailable(from, to, date);
            }
            rateCache.put(from, to, date, providerRate.getValue());
        }
    }

    private boolean isRangeCached(CurrencyCode from, CurrencyCode to, LocalDate startDate, LocalDate endDate) {
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (rateCache.find(from, to, date).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private CurrencyConversionUnavailableException unavailable(
        CurrencyCode from,
        CurrencyCode to,
        LocalDate date
    ) {
        return new CurrencyConversionUnavailableException(
            "Currency conversion is temporarily unavailable for " + from + " to " + to + " on " + date
        );
    }
}
