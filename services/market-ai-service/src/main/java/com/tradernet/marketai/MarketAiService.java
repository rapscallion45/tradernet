package com.tradernet.marketai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.marketai.engine.AiSignalEngine;
import com.tradernet.marketai.engine.BarAggregator;
import com.tradernet.marketai.engine.FeatureEngine;
import com.tradernet.marketai.engine.MarketEventPublisher;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.ChartInterval;
import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.marketai.model.MarketTrade;
import com.tradernet.marketai.stream.BinanceTradeStreamClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Coordinates market data ingestion, feature generation and signal publishing.
 */
@Singleton
@Startup
public class MarketAiService {

    private static final int DEFAULT_HISTORY_SIZE = 2_000;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BinanceTradeStreamClient binanceClient = new BinanceTradeStreamClient();
    private final BarAggregator barAggregator = new BarAggregator(1_000L);
    private final FeatureEngine featureEngine = new FeatureEngine();
    private final AiSignalEngine signalEngine = new AiSignalEngine();
    private final MarketEventPublisher publisher = new MarketEventPublisher();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private final Deque<MarketBar> bars = new ArrayDeque<>();
    private final Deque<AiSignal> signals = new ArrayDeque<>();

    @PostConstruct
    public void start() {
        final String symbol = System.getProperty("market.ai.symbol", "btcusdt");
        binanceClient.start(symbol, this::onTrade);
    }

    @PreDestroy
    public void stop() {
        binanceClient.stop();
    }

    public synchronized List<MarketBar> getBars(int limit) {
        return takeLast(bars, limit);
    }

    public List<MarketBar> getBars(String symbol, String intervalToken, int limit) {
        final ChartInterval interval = ChartInterval.parse(intervalToken);
        final List<MarketBar> remoteBars = fetchKlines(symbol, interval, limit);
        if (!remoteBars.isEmpty()) {
            return remoteBars;
        }

        synchronized (this) {
            return takeLast(bars, limit);
        }
    }

    public synchronized List<AiSignal> getSignals(int limit) {
        return takeLast(signals, limit);
    }

    public AutoCloseable subscribeBars(Consumer<MarketBar> consumer) {
        return publisher.onBar(consumer);
    }

    public AutoCloseable subscribeSignals(Consumer<AiSignal> consumer) {
        return publisher.onSignal(consumer);
    }

    private void onTrade(MarketTrade trade) {
        final MarketBar closed = barAggregator.ingest(trade);
        final MarketBar forming = barAggregator.snapshotForming();
        if (forming != null) {
            publisher.publishBar(forming);
        }

        if (closed == null) {
            return;
        }

        synchronized (this) {
            appendBounded(bars, closed, DEFAULT_HISTORY_SIZE);
        }

        final FeatureSnapshot features = featureEngine.onClosedBar(closed);
        final AiSignal signal = signalEngine.evaluate(features);
        if (signal == null) {
            return;
        }

        synchronized (this) {
            appendBounded(signals, signal, DEFAULT_HISTORY_SIZE);
        }
        publisher.publishSignal(signal);
    }

    private List<MarketBar> fetchKlines(String symbol, ChartInterval interval, int limit) {
        final String normalizedSymbol = normalizeSymbol(symbol);
        final int boundedLimit = Math.max(1, Math.min(limit, 1_000));
        final String endpoint = "https://api.binance.com/api/v3/klines?symbol="
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

            final JsonNode payload = OBJECT_MAPPER.readTree(response.body());
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

    private String normalizeSymbol(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            return "BTCUSDT";
        }

        final String upper = rawSymbol.trim().toUpperCase();
        if ("BTCUSD".equals(upper)) {
            return "BTCUSDT";
        }
        if ("ETHUSD".equals(upper)) {
            return "ETHUSDT";
        }
        return upper;
    }

    private <T> void appendBounded(Deque<T> deque, T value, int maxSize) {
        deque.addLast(value);
        while (deque.size() > maxSize) {
            deque.removeFirst();
        }
    }

    private <T> List<T> takeLast(Deque<T> deque, int limit) {
        final int size = deque.size();
        final int effectiveLimit = Math.max(1, limit);
        final int skip = Math.max(0, size - effectiveLimit);
        final List<T> snapshot = new ArrayList<>(size);
        int index = 0;
        for (T item : deque) {
            if (index++ >= skip) {
                snapshot.add(item);
            }
        }
        return snapshot;
    }
}
