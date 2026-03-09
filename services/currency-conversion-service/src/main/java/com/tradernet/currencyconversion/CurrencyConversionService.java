package com.tradernet.currencyconversion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.marketai.model.MarketBar;
import jakarta.ejb.Singleton;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.EnumMap;

@Singleton
public class CurrencyConversionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final MathContext MC = MathContext.DECIMAL64;

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
    private final Map<String, BigDecimal> rateCache = new ConcurrentHashMap<>();

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
        return BigDecimal.valueOf(amount).multiply(rate, MC).doubleValue();
    }

    public MarketBar convertBar(MarketBar bar, CurrencyCode targetCurrency) {
        if (bar == null) {
            return null;
        }

        CurrencyCode sourceCurrency = resolveQuoteCurrency(bar.getSymbol());
        Instant timestamp = Instant.ofEpochMilli(bar.getBucketStart());

        if (sourceCurrency == targetCurrency) {
            return bar;
        }

        return new MarketBar(
            bar.getSymbol(),
            bar.getBucketStart(),
            convertAmount(bar.getOpen(), sourceCurrency, targetCurrency, timestamp),
            convertAmount(bar.getHigh(), sourceCurrency, targetCurrency, timestamp),
            convertAmount(bar.getLow(), sourceCurrency, targetCurrency, timestamp),
            convertAmount(bar.getClose(), sourceCurrency, targetCurrency, timestamp),
            bar.getVolume(),
            bar.isClosed()
        );
    }

    public BigDecimal getRate(CurrencyCode from, CurrencyCode to, Instant timestamp) {
        if (from == to) {
            return BigDecimal.ONE;
        }

        String cacheKey = from.name() + "_" + to.name() + "_" + LocalDate.ofInstant(timestamp, ZoneOffset.UTC);
        BigDecimal cached = rateCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        BigDecimal direct = fetchRate(from, to, timestamp);
        if (direct != null) {
            rateCache.put(cacheKey, direct);
            return direct;
        }

        if (from != CurrencyCode.USD && to != CurrencyCode.USD) {
            BigDecimal fromToUsd = getRate(from, CurrencyCode.USD, timestamp);
            BigDecimal usdToTarget = getRate(CurrencyCode.USD, to, timestamp);
            BigDecimal cross = fromToUsd.multiply(usdToTarget, MC);
            rateCache.put(cacheKey, cross);
            return cross;
        }

        BigDecimal fallback = getFallbackRate(from, to);
        rateCache.put(cacheKey, fallback);
        return fallback;
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
        LocalDate date = LocalDate.ofInstant(timestamp, ZoneOffset.UTC);
        String uri = "https://api.frankfurter.app/" + date + "?from=" + encode(from.name()) + "&to=" + encode(to.name());
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
            JsonNode rates = root.path("rates");
            if (!rates.has(to.name())) {
                return null;
            }

            BigDecimal value = rates.path(to.name()).decimalValue();
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

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
