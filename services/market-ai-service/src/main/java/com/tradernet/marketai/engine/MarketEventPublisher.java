package com.tradernet.marketai.engine;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.MarketBar;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-process, symbol-keyed pub/sub for bar and signal updates.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(MarketEventPublisher.class);

    private final Map<String, CopyOnWriteArrayList<Consumer<MarketBar>>> barListeners = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<Consumer<AiSignal>>> signalListeners = new ConcurrentHashMap<>();

    public AutoCloseable onBar(String symbol, Consumer<MarketBar> listener) {
        return addListener(barListeners, symbol, listener);
    }

    public AutoCloseable onSignal(String symbol, Consumer<AiSignal> listener) {
        return addListener(signalListeners, symbol, listener);
    }

    public void publishBar(MarketBar bar) {
        if (bar != null) {
            publish(barListeners, bar.getSymbol(), bar, "market bar");
        }
    }

    public void publishSignal(AiSignal signal) {
        if (signal != null) {
            publish(signalListeners, signal.getSymbol(), signal, "market signal");
        }
    }

    private <T> AutoCloseable addListener(
        Map<String, CopyOnWriteArrayList<Consumer<T>>> listenersBySymbol,
        String symbol,
        Consumer<T> listener
    ) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final CopyOnWriteArrayList<Consumer<T>> listeners = listenersBySymbol.computeIfAbsent(
            normalizedSymbol,
            ignored -> new CopyOnWriteArrayList<>()
        );
        listeners.add(listener);
        return () -> {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                listenersBySymbol.remove(normalizedSymbol, listeners);
            }
        };
    }

    private <T> void publish(
        Map<String, CopyOnWriteArrayList<Consumer<T>>> listenersBySymbol,
        String symbol,
        T event,
        String eventName
    ) {
        final List<Consumer<T>> listeners = listenersBySymbol.get(MarketSymbolNormalizer.normalizeSymbol(symbol));
        if (listeners == null) {
            return;
        }
        for (Consumer<T> listener : listeners) {
            try {
                listener.accept(event);
            } catch (RuntimeException ex) {
                listeners.remove(listener);
                LOG.warn("Removed failing {} listener after callback error.", eventName, ex);
            }
        }
    }
}
