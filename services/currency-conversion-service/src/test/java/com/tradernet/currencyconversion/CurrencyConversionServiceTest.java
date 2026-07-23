package com.tradernet.currencyconversion;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyConversionServiceTest {

    @Test
    void cachesProviderSourcedRates() {
        final StubGateway gateway = new StubGateway(new BigDecimal("0.80"));
        final CurrencyConversionService service = new CurrencyConversionService(gateway, new FxRateCache());
        final Instant timestamp = LocalDate.of(2024, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC);

        assertEquals(new BigDecimal("0.80"), service.getRate(CurrencyCode.USD, CurrencyCode.GBP, timestamp));
        assertEquals(new BigDecimal("0.80"), service.getRate(CurrencyCode.USD, CurrencyCode.GBP, timestamp));
        assertEquals(1, gateway.singleRateRequests);
    }

    @Test
    void reportsUnavailableRatesInsteadOfInventingFallbackValues() {
        final CurrencyConversionService service = new CurrencyConversionService(
            new StubGateway(null),
            new FxRateCache()
        );
        final Instant timestamp = LocalDate.of(2024, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC);

        assertThrows(
            CurrencyConversionUnavailableException.class,
            () -> service.getRate(CurrencyCode.USD, CurrencyCode.GBP, timestamp)
        );
    }

    private static final class StubGateway implements FxRateGateway {
        private final BigDecimal rate;
        private int singleRateRequests;

        private StubGateway(BigDecimal rate) {
            this.rate = rate;
        }

        @Override
        public Optional<BigDecimal> findRate(CurrencyCode from, CurrencyCode to, LocalDate date) {
            singleRateRequests += 1;
            return Optional.ofNullable(rate);
        }

        @Override
        public NavigableMap<LocalDate, BigDecimal> findRates(
            CurrencyCode from,
            CurrencyCode to,
            LocalDate startDate,
            LocalDate endDate
        ) {
            final NavigableMap<LocalDate, BigDecimal> rates = new TreeMap<>();
            if (rate != null) {
                rates.put(endDate, rate);
            }
            return rates;
        }
    }
}
