package com.tradernet.marketai;

import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.MarketBar;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Thread-safe bounded in-memory history for live bars and AI signals.
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketHistoryBuffer {

    private static final int DEFAULT_MAX_SIZE = 2_000;

    private final int maxSize = DEFAULT_MAX_SIZE;
    private final Deque<MarketBar> bars = new ArrayDeque<>();
    private final Deque<AiSignal> signals = new ArrayDeque<>();

    @Lock(LockType.WRITE)
    public void appendBar(MarketBar bar) {
        appendBounded(bars, bar, maxSize);
    }

    @Lock(LockType.WRITE)
    public void appendSignal(AiSignal signal) {
        appendBounded(signals, signal, maxSize);
    }

    @Lock(LockType.READ)
    public List<MarketBar> getBars(int limit) {
        return takeLast(bars, limit);
    }

    @Lock(LockType.READ)
    public List<MarketBar> getBarsForSymbol(String normalizedSymbol, int limit) {
        return takeLast(bars.stream()
            .filter(bar -> matchesSymbol(bar.getSymbol(), normalizedSymbol))
            .collect(Collectors.toList()), limit);
    }

    @Lock(LockType.READ)
    public List<AiSignal> getSignals(int limit) {
        return takeLast(signals, limit);
    }

    @Lock(LockType.READ)
    public List<AiSignal> getSignalsForSymbol(String normalizedSymbol, int limit) {
        return takeLast(signals.stream()
            .filter(signal -> matchesSymbol(signal.getSymbol(), normalizedSymbol))
            .collect(Collectors.toList()), limit);
    }

    public static <T> List<T> takeLast(List<T> values, int limit) {
        final int size = values.size();
        final int effectiveLimit = Math.max(1, limit);
        final int skip = Math.max(0, size - effectiveLimit);
        final List<T> snapshot = new ArrayList<>(size);
        int index = 0;
        for (T item : values) {
            if (index++ >= skip) {
                snapshot.add(item);
            }
        }
        return snapshot;
    }

    private static <T> void appendBounded(Deque<T> deque, T value, int maxSize) {
        deque.addLast(value);
        while (deque.size() > maxSize) {
            deque.removeFirst();
        }
    }

    private static <T> List<T> takeLast(Deque<T> deque, int limit) {
        return takeLast(new ArrayList<>(deque), limit);
    }

    private static boolean matchesSymbol(String actualSymbol, String normalizedSymbol) {
        return actualSymbol != null && actualSymbol.trim().toUpperCase(Locale.ROOT).equals(normalizedSymbol);
    }
}
