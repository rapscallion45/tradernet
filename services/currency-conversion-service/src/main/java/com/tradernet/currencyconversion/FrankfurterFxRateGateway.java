package com.tradernet.currencyconversion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Reads daily FX rates from the Frankfurter provider.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class FrankfurterFxRateGateway implements FxRateGateway {

    private static final Logger LOG = LoggerFactory.getLogger(FrankfurterFxRateGateway.class);
    private static final String RATES_URL = "https://api.frankfurter.dev/v2/rates";

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Optional<BigDecimal> findRate(CurrencyCode from, CurrencyCode to, LocalDate date) {
        final NavigableMap<LocalDate, BigDecimal> rates = findRates(from, to, date.minusDays(7), date);
        return rates.isEmpty() ? Optional.empty() : Optional.of(rates.lastEntry().getValue());
    }

    @Override
    public NavigableMap<LocalDate, BigDecimal> findRates(
        CurrencyCode from,
        CurrencyCode to,
        LocalDate startDate,
        LocalDate endDate
    ) {
        final String uri = RATES_URL
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
                LOG.warn("FX provider returned HTTP {} for {} to {}.", response.statusCode(), from, to);
                return rates;
            }

            final JsonNode root = objectMapper.readTree(response.body());
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
            LOG.warn("Unable to read FX rates for {} to {} from {} through {}.", from, to, startDate, endDate, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return rates;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
