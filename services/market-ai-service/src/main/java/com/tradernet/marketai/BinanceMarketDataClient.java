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

    private static final int MAX_KLINES_PER_REQUEST = 1_000;

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
        final int boundedLimit = Math.max(1, Math.min(limit, MarketBarProvider.MAX_BARS));
        final List<MarketBar> result = new ArrayList<>(boundedLimit);
        Long endTime = null;

        while (result.size() < boundedLimit) {
            final int pageLimit = Math.min(MAX_KLINES_PER_REQUEST, boundedLimit - result.size());
            final List<MarketBar> page = fetchKlinePage(normalizedSymbol, interval, pageLimit, endTime);
            if (page.isEmpty()) {
                break;
            }
            if (endTime != null && page.get(page.size() - 1).getBucketStart() > endTime) {
                break;
            }

            result.addAll(0, page);
            if (page.size() < pageLimit) {
                break;
            }

            final long earliestBucketStart = page.get(0).getBucketStart();
            if (earliestBucketStart <= 0L) {
                break;
            }
            final long nextEndTime = earliestBucketStart - 1L;
            if (endTime != null && nextEndTime >= endTime) {
                break;
            }
            endTime = nextEndTime;
        }

        return result;
    }

    protected List<MarketBar> fetchKlinePage(
        String normalizedSymbol,
        ChartInterval interval,
        int limit,
        Long endTime
    ) {
        final String endpoint = getBinanceRestBaseUrl() + "/api/v3/klines?symbol="
                + URLEncoder.encode(normalizedSymbol, StandardCharsets.UTF_8)
                + "&interval=" + URLEncoder.encode(interval.getBinanceInterval(), StandardCharsets.UTF_8)
                + "&limit=" + limit
                + (endTime == null ? "" : "&endTime=" + endTime);

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

    protected String getBinanceRestBaseUrl() {
        return configuration.getBinanceRestBaseUrl();
    }
}
