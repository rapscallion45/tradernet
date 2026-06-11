package com.tradernet.marketai.forecast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.marketai.model.MarketContextSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * HTTP bridge to the Python TimesFM/Chronos forecasting service.
 */
public class ForecastingClient {

    private static final Logger LOG = LoggerFactory.getLogger(ForecastingClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI baseUri;

    public ForecastingClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUri = URI.create(System.getProperty("market.ai.forecasting.url", "http://forecasting-service:8000"));
    }

    public MarketForecast forecast(String symbol, int horizonDays, MarketContextSnapshot context) {
        final String normalizedSymbol = symbol == null || symbol.isBlank() ? "BTCUSDT" : symbol.trim().toUpperCase();
        final int boundedHorizonDays = Math.max(1, Math.min(horizonDays, 365));
        final URI uri = baseUri.resolve("/forecast?symbol=" + encode(normalizedSymbol) + "&horizon_days=" + boundedHorizonDays);
        final HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                LOG.warn("Forecasting service returned status {} for {}", response.statusCode(), normalizedSymbol);
                return fallback(normalizedSymbol, boundedHorizonDays, context, "forecasting_service_status_" + response.statusCode());
            }
            return parseForecast(response.body(), normalizedSymbol, boundedHorizonDays, context);
        } catch (IOException ex) {
            LOG.warn("Unable to reach forecasting service for {}", normalizedSymbol, ex);
            return fallback(normalizedSymbol, boundedHorizonDays, context, "forecasting_service_unreachable");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.warn("Forecasting service call interrupted for {}", normalizedSymbol, ex);
            return fallback(normalizedSymbol, boundedHorizonDays, context, "forecasting_service_interrupted");
        } catch (RuntimeException ex) {
            LOG.warn("Unable to parse forecasting response for {}", normalizedSymbol, ex);
            return fallback(normalizedSymbol, boundedHorizonDays, context, "forecasting_service_parse_error");
        }
    }

    private MarketForecast parseForecast(String body, String symbol, int horizonDays, MarketContextSnapshot context) throws IOException {
        final JsonNode root = objectMapper.readTree(body);
        final double probability = clamp(root.path("probability_positive_return").asDouble(0.5), 0.0, 1.0);
        final double expectedReturn = root.path("expected_return").asDouble(0.0);
        final double bullScore = clamp(root.path("bull_score").asDouble(scoreFromProbability(probability)), 0.0, 100.0);
        final String model = root.path("model").asText("forecasting-service");
        final List<String> drivers = new ArrayList<>();
        final JsonNode driverNode = root.path("drivers");
        if (driverNode.isArray()) {
            for (JsonNode node : driverNode) {
                drivers.add(node.asText());
            }
        }
        addContextDrivers(drivers, context);
        return new MarketForecast(symbol, horizonDays, probability, expectedReturn, bullScore, model, drivers, null);
    }

    private MarketForecast fallback(String symbol, int horizonDays, MarketContextSnapshot context, String reason) {
        final List<String> drivers = new ArrayList<>();
        drivers.add(reason);
        addContextDrivers(drivers, context);
        final double bullScore = clamp(50.0 + contextScore(context) * 10.0, 0.0, 100.0);
        final double probability = clamp(0.5 + (bullScore - 50.0) / 100.0, 0.05, 0.95);
        return new MarketForecast(symbol, horizonDays, probability, 0.0, bullScore, "context-fallback", drivers, null);
    }

    private void addContextDrivers(List<String> drivers, MarketContextSnapshot context) {
        if (context == null) {
            return;
        }
        if (context.getEtfFlowZScore() > 0.25) {
            drivers.add("ETF inflows positive");
        } else if (context.getEtfFlowZScore() < -0.25) {
            drivers.add("ETF flows negative");
        }
        if (context.getExchangeOutflowZScore() > 0.25) {
            drivers.add("exchange balances declining");
        }
        if (Math.abs(context.getFundingRateZScore()) <= 0.5) {
            drivers.add("funding rates neutral");
        } else if (context.getFundingRateZScore() > 1.0) {
            drivers.add("funding rates elevated");
        }
    }

    private double contextScore(MarketContextSnapshot context) {
        if (context == null) {
            return 0.0;
        }
        return clamp((context.getEtfFlowZScore() + context.getExchangeOutflowZScore() - Math.max(context.getFundingRateZScore(), 0.0)
                + context.getLiquidityGrowthZScore() + context.getSentimentZScore()) / 5.0, -2.0, 2.0);
    }

    private double scoreFromProbability(double probability) {
        return 50.0 + (probability - 0.5) * 100.0;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
