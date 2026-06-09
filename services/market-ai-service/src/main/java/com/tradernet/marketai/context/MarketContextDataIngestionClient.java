package com.tradernet.marketai.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.marketai.model.MarketContextSnapshot;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Fetches no-key market context data that can be hydrated directly from the Java app.
 */
public class MarketContextDataIngestionClient {

    private static final String BINANCE_FUTURES_BASE_URL = "https://fapi.binance.com";
    private static final String FEAR_AND_GREED_URL = "https://api.alternative.me/fng/?limit=30&format=json";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MarketContextDataIngestionClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public MarketContextSnapshot fetch(String symbol, MarketContextSnapshot current) {
        final MarketContextSnapshot next = copy(current);
        boolean available = current != null && current.isAvailable();

        final OptionalDouble fundingRateZScore = fetchFundingRateZScore(symbol);
        if (fundingRateZScore.isPresent()) {
            next.setFundingRateZScore(fundingRateZScore.getAsDouble());
            available = true;
        }

        final OptionalDouble openInterestChangeZScore = fetchOpenInterestChangeZScore(symbol);
        if (openInterestChangeZScore.isPresent()) {
            next.setOpenInterestChangeZScore(openInterestChangeZScore.getAsDouble());
            available = true;
        }

        final OptionalDouble sentimentZScore = fetchSentimentZScore();
        if (sentimentZScore.isPresent()) {
            next.setSentimentZScore(sentimentZScore.getAsDouble());
            available = true;
        }

        next.setAvailable(available);
        return next;
    }

    private OptionalDouble fetchFundingRateZScore(String symbol) {
        final String url = BINANCE_FUTURES_BASE_URL + "/fapi/v1/fundingRate?symbol=" + encode(symbol) + "&limit=90";
        final JsonNode response = getJson(url);
        if (!response.isArray()) {
            return OptionalDouble.empty();
        }

        final List<Double> rates = new ArrayList<>();
        response.forEach(node -> addDouble(rates, node.path("fundingRate").asText(null)));
        return latestZScore(rates);
    }

    private OptionalDouble fetchOpenInterestChangeZScore(String symbol) {
        final String url = BINANCE_FUTURES_BASE_URL + "/futures/data/openInterestHist?symbol=" + encode(symbol) + "&period=1d&limit=30";
        final JsonNode response = getJson(url);
        if (!response.isArray()) {
            return OptionalDouble.empty();
        }

        final List<Double> values = new ArrayList<>();
        response.forEach(node -> addDouble(values, node.path("sumOpenInterest").asText(null)));
        if (values.size() < 3) {
            return OptionalDouble.empty();
        }

        final List<Double> changes = new ArrayList<>();
        for (int index = 1; index < values.size(); index += 1) {
            final double previous = values.get(index - 1);
            final double current = values.get(index);
            if (previous > 0.0) {
                changes.add((current - previous) / previous);
            }
        }
        return latestZScore(changes);
    }

    private OptionalDouble fetchSentimentZScore() {
        final JsonNode response = getJson(FEAR_AND_GREED_URL);
        final JsonNode data = response.path("data");
        if (!data.isArray() || data.isEmpty()) {
            return OptionalDouble.empty();
        }

        final List<Double> values = new ArrayList<>();
        data.forEach(node -> addDouble(values, node.path("value").asText(null)));
        if (values.isEmpty()) {
            return OptionalDouble.empty();
        }

        final double latest = values.get(0);
        return OptionalDouble.of(clamp((latest - 50.0) / 25.0, -2.0, 2.0));
    }

    private JsonNode getJson(String url) {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(response.body());
        } catch (IOException ex) {
            return objectMapper.createObjectNode();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return objectMapper.createObjectNode();
        } catch (RuntimeException ex) {
            return objectMapper.createObjectNode();
        }
    }

    private OptionalDouble latestZScore(List<Double> values) {
        if (values.size() < 2) {
            return OptionalDouble.empty();
        }

        final double latest = values.get(values.size() - 1);
        final double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        final double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2.0))
                .average()
                .orElse(0.0);
        final double standardDeviation = Math.sqrt(variance);
        if (standardDeviation <= 0.000000001) {
            return OptionalDouble.of(0.0);
        }
        return OptionalDouble.of(clamp((latest - mean) / standardDeviation, -2.0, 2.0));
    }

    private MarketContextSnapshot copy(MarketContextSnapshot source) {
        if (source == null) {
            return MarketContextSnapshot.neutral();
        }
        return new MarketContextSnapshot(
                source.getEtfFlowZScore(),
                source.getExchangeOutflowZScore(),
                source.getFundingRateZScore(),
                source.getOpenInterestChangeZScore(),
                source.getMvrvZScore(),
                source.getLiquidityGrowthZScore(),
                source.getSentimentZScore(),
                source.isAvailable()
        );
    }

    private void addDouble(List<Double> values, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            values.add(Double.parseDouble(value));
        } catch (NumberFormatException ex) {
            // Skip malformed provider data points.
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
