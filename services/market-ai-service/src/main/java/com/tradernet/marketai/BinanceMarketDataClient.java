package com.tradernet.marketai;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.marketai.model.ChartInterval;
import com.tradernet.marketai.model.MarketBar;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reads market symbols and historical klines from Binance REST APIs.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class BinanceMarketDataClient {

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @EJB
    private MarketAiConfiguration configuration;

    public List<String> fetchExchangeSymbols() {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(getBinanceRestBaseUrl() + "/api/v3/exchangeInfo"))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                return List.of();
            }

            final JsonNode payload = objectMapper.readTree(response.body());
            final JsonNode symbolsNode = payload.get("symbols");
            if (symbolsNode == null || !symbolsNode.isArray()) {
                return List.of();
            }

            final List<String> result = new ArrayList<>(symbolsNode.size());
            for (JsonNode node : symbolsNode) {
                if (!"TRADING".equals(node.path("status").asText())) {
                    continue;
                }

                final String symbol = node.path("symbol").asText("").trim().toUpperCase();
                if (!symbol.isEmpty()) {
                    result.add(symbol);
                }
            }

            result.sort(Comparator.naturalOrder());
            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (IOException ex) {
            return List.of();
        }
    }

    public List<MarketBar> fetchKlines(String symbol, ChartInterval interval, int limit) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final int boundedLimit = Math.max(1, Math.min(limit, 1_000));
        final String endpoint = getBinanceRestBaseUrl() + "/api/v3/klines?symbol="
                + URLEncoder.encode(normalizedSymbol, StandardCharsets.UTF_8)
                + "&interval=" + URLEncoder.encode(interval.getBinanceInterval(), StandardCharsets.UTF_8)
                + "&limit=" + boundedLimit;

        final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                return List.of();
            }

            final JsonNode payload = objectMapper.readTree(response.body());
            if (!payload.isArray()) {
                return List.of();
            }

            final List<MarketBar> result = new ArrayList<>(payload.size());
            for (JsonNode node : payload) {
                if (!node.isArray() || node.size() < 6) {
                    continue;
                }

                final long bucketStart = node.get(0).asLong();
                final double open = node.get(1).asDouble();
                final double high = node.get(2).asDouble();
                final double low = node.get(3).asDouble();
                final double close = node.get(4).asDouble();
                final double volume = node.get(5).asDouble();
                result.add(new MarketBar(normalizedSymbol, bucketStart, open, high, low, close, volume, true));
            }

            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private String getBinanceRestBaseUrl() {
        return configuration.getBinanceRestBaseUrl();
    }
}
