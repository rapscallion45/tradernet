package com.tradernet.currencyconversion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.EnumMap;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CurrencyConversionService {

    private static final Logger LOG = LoggerFactory.getLogger(CurrencyConversionService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final int CURRENCY_SCALE = 2;
    private static final long CURRENT_RATE_TTL_MS = Duration.ofHours(1).toMillis();
    private static final long FALLBACK_RATE_TTL_MS = Duration.ofMinutes(1).toMillis();
    private static final String FRANKFURTER_V2_RATES_URL = "https://api.frankfurter.dev/v2/rates";

    private static final Map<CurrencyCode, BigDecimal> USD_BASED_FALLBACK_RATES = new EnumMap<>(CurrencyCode.class);

    static {
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.USD, BigDecimal.ONE);
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.EUR, new BigDecimal("0.92"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.GBP, new BigDecimal("0.79"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.JPY, new BigDecimal("148.00"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.CAD, new BigDecimal("1.35"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.AUD, new BigDecimal("1.53"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.CHF, new BigDecimal("0.89"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.INR, new BigDecimal("83.00"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.BRL, new BigDecimal("5.00"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.MXN, new BigDecimal("16.80"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.CNY, new BigDecimal("7.20"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.KRW, new BigDecimal("1330.00"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.SGD, new BigDecimal("1.34"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.HKD, new BigDecimal("7.80"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.ZAR, new BigDecimal("18.50"));
        USD_BASED_FALLBACK_RATES.put(CurrencyCode.AED, new BigDecimal("3.67"));
    }

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final Map<String, CachedRate> rateCache = new ConcurrentHashMap<>();

    public List<String> getSupportedCurrencies() {
        final List<String> supported = new ArrayList<>();
        for (CurrencyCode currencyCode : CurrencyCode.values()) {
            supported.add(currencyCode.name());
        }
        supported.sort(Comparator.naturalOrder());
        return supported;
    }

    public CurrencyCode resolveQuoteCurrency(String symbol) {
        if (symbol == null) {
            return CurrencyCode.USD;
        }

        String upper = symbol.trim().toUpperCase();
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

    public double convertAmount(double amount, CurrencyCode from, CurrencyCode to, Instant timestamp) {
        if (from == to) {
            return amount;
        }

        BigDecimal rate = getRate(from, to, timestamp == null ? Instant.now() : timestamp);
        return BigDecimal.valueOf(amount)
            .multiply(rate, MC)
            .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)
            .doubleValue();
    }

    public BigDecimal getRate(CurrencyCode from, CurrencyCode to, Instant timestamp) {
        if (from == to) {
            return BigDecimal.ONE;
        }

        String cacheKey = from.name() + "_" + to.name() + "_" + LocalDate.ofInstant(timestamp, ZoneOffset.UTC);
        CachedRate cached = rateCache.get(cacheKey);
        if (cached != null && cached.isFresh()) {
            return cached.rate;
        }
        if (cached != null) {
            rateCache.remove(cacheKey, cached);
        }

        BigDecimal direct = fetchRate(from, to, timestamp);
        if (direct != null) {
            cacheProviderRate(cacheKey, direct, LocalDate.ofInstant(timestamp, ZoneOffset.UTC));
            return direct;
        }

        if (from != CurrencyCode.USD && to != CurrencyCode.USD) {
            BigDecimal fromToUsd = getRate(from, CurrencyCode.USD, timestamp);
            BigDecimal usdToTarget = getRate(CurrencyCode.USD, to, timestamp);
            BigDecimal cross = fromToUsd.multiply(usdToTarget, MC);
            rateCache.put(cacheKey, CachedRate.fallback(cross));
            return cross;
        }

        BigDecimal fallback = getFallbackRate(from, to);
        rateCache.put(cacheKey, CachedRate.fallback(fallback));
        LOG.warn("Using short-lived fallback FX rate for {} to {} on {}.", from, to, LocalDate.ofInstant(timestamp, ZoneOffset.UTC));
        return fallback;
    }

    /**
     * Warms a complete daily rate range with one provider request.
     */
    public void prefetchRates(CurrencyCode from, CurrencyCode to, Instant start, Instant end) {
        if (from == to || start == null || end == null) {
            return;
        }

        final LocalDate startDate = LocalDate.ofInstant(start, ZoneOffset.UTC);
        final LocalDate endDate = LocalDate.ofInstant(end, ZoneOffset.UTC);
        if (startDate.isAfter(endDate)) {
            return;
        }

        final NavigableMap<LocalDate, BigDecimal> providerRates = fetchRateRange(
            from,
            to,
            startDate.minusDays(7),
            endDate
        );
        if (providerRates.isEmpty()) {
            cacheFallbackRange(from, to, startDate, endDate);
            return;
        }

        boolean usedFallback = false;
        final BigDecimal fallback = getFallbackRate(from, to);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final Map.Entry<LocalDate, BigDecimal> floor = providerRates.floorEntry(date);
            if (floor != null) {
                cacheProviderRate(cacheKey(from, to, date), floor.getValue(), date);
            } else {
                rateCache.put(cacheKey(from, to, date), CachedRate.fallback(fallback));
                usedFallback = true;
            }
        }
        if (usedFallback) {
            LOG.warn("Using short-lived fallback FX rates before the first provider quote for {} to {} from {}.",
                from, to, startDate);
        }
    }

    private BigDecimal getFallbackRate(CurrencyCode from, CurrencyCode to) {
        if (from == to) {
            return BigDecimal.ONE;
        }

        BigDecimal fromPerUsd = USD_BASED_FALLBACK_RATES.get(from);
        BigDecimal toPerUsd = USD_BASED_FALLBACK_RATES.get(to);

        if (fromPerUsd == null || toPerUsd == null || fromPerUsd.signum() <= 0) {
            return BigDecimal.ONE;
        }

        return toPerUsd.divide(fromPerUsd, MC);
    }

    private BigDecimal fetchRate(CurrencyCode from, CurrencyCode to, Instant timestamp) {
        final LocalDate date = LocalDate.ofInstant(timestamp, ZoneOffset.UTC);
        String uri = FRANKFURTER_V2_RATES_URL + "?from=" + date.minusDays(7)
            + "&to=" + date
            + "&base=" + encode(from.name())
            + "&quotes=" + encode(to.name());
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
            .timeout(Duration.ofSeconds(4))
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                return null;
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) {
                return null;
            }

            BigDecimal value = root.get(root.size() - 1).path("rate").decimalValue();
            if (value.signum() <= 0) {
                return null;
            }
            return value;
        } catch (IOException ex) {
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private NavigableMap<LocalDate, BigDecimal> fetchRateRange(
        CurrencyCode from,
        CurrencyCode to,
        LocalDate startDate,
        LocalDate endDate
    ) {
        final String uri = FRANKFURTER_V2_RATES_URL
            + "?from=" + startDate
            + "&to=" + endDate
            + "&base=" + encode(from.name())
            + "&quotes=" + encode(to.name());
        final HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
            .timeout(Duration.ofSeconds(12))
            .GET()
            .build();

        final NavigableMap<LocalDate, BigDecimal> rates = new TreeMap<>();
        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                return rates;
            }

            final JsonNode root = OBJECT_MAPPER.readTree(response.body());
            if (!root.isArray()) {
                return rates;
            }
            for (JsonNode item : root) {
                if (!to.name().equalsIgnoreCase(item.path("quote").asText())) {
                    continue;
                }
                final BigDecimal rate = item.path("rate").decimalValue();
                if (rate.signum() > 0) {
                    rates.put(LocalDate.parse(item.path("date").asText()), rate);
                }
            }
        } catch (IOException | RuntimeException ex) {
            LOG.warn("Unable to prefetch FX rates for {} to {} from {} through {}.", from, to, startDate, endDate, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return rates;
    }

    private void cacheFallbackRange(CurrencyCode from, CurrencyCode to, LocalDate startDate, LocalDate endDate) {
        final BigDecimal fallback = getFallbackRate(from, to);
        LOG.warn("Using short-lived fallback FX range for {} to {} from {} through {}.", from, to, startDate, endDate);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            rateCache.put(cacheKey(from, to, date), CachedRate.fallback(fallback));
        }
    }

    private void cacheProviderRate(String cacheKey, BigDecimal rate, LocalDate date) {
        final boolean currentDate = date.equals(LocalDate.now(ZoneOffset.UTC));
        rateCache.put(cacheKey, CachedRate.provider(rate, currentDate ? CURRENT_RATE_TTL_MS : Long.MAX_VALUE));
    }

    private String cacheKey(CurrencyCode from, CurrencyCode to, LocalDate date) {
        return from.name() + "_" + to.name() + "_" + date;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static final class CachedRate {
        private final BigDecimal rate;
        private final long expiresAtMs;

        private CachedRate(BigDecimal rate, long expiresAtMs) {
            this.rate = rate;
            this.expiresAtMs = expiresAtMs;
        }

        private static CachedRate provider(BigDecimal rate, long ttlMs) {
            return new CachedRate(rate, ttlMs == Long.MAX_VALUE ? Long.MAX_VALUE : System.currentTimeMillis() + ttlMs);
        }

        private static CachedRate fallback(BigDecimal rate) {
            return new CachedRate(rate, System.currentTimeMillis() + FALLBACK_RATE_TTL_MS);
        }

        private boolean isFresh() {
            return expiresAtMs == Long.MAX_VALUE || System.currentTimeMillis() < expiresAtMs;
        }
    }
}
