package com.tradernet.currencyconversion;

import jakarta.ejb.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NavigableMap;
import java.util.Optional;

/**
 * Internal provider gateway for authoritative FX rates.
 */
@Local
public interface FxRateGateway {

    Optional<BigDecimal> findRate(CurrencyCode from, CurrencyCode to, LocalDate date);

    NavigableMap<LocalDate, BigDecimal> findRates(
        CurrencyCode from,
        CurrencyCode to,
        LocalDate startDate,
        LocalDate endDate
    );
}
