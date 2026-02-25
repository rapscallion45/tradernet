package com.tradernet.marketai;

import com.tradernet.marketai.engine.AiSignalEngine;
import com.tradernet.marketai.engine.BarAggregator;
import com.tradernet.marketai.engine.FeatureEngine;
import com.tradernet.marketai.engine.MarketEventPublisher;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.marketai.model.MarketTrade;
import com.tradernet.marketai.stream.BinanceTradeStreamClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

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

    private final BinanceTradeStreamClient binanceClient = new BinanceTradeStreamClient();
    private final BarAggregator barAggregator = new BarAggregator(1_000L);
    private final FeatureEngine featureEngine = new FeatureEngine();
    private final AiSignalEngine signalEngine = new AiSignalEngine();
    private final MarketEventPublisher publisher = new MarketEventPublisher();

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
