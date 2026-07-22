package com.tradernet.currencyconversion;

import jakarta.ejb.Local;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Cross-module contract for provider-backed currency conversion.
 */
@Local
public interface CurrencyConversionProvider {

    List<String> getSupportedCurrencies();

    CurrencyCode resolveQuoteCurrency(String symbol);

    double convertAmount(double amount, CurrencyCode from, CurrencyCode to, Instant timestamp);

    BigDecimal getRate(CurrencyCode from, CurrencyCode to, Instant timestamp);

    void prefetchRates(CurrencyCode from, CurrencyCode to, Instant start, Instant end);
}
