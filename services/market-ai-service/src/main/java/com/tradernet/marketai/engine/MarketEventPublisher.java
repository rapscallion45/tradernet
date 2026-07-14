package com.tradernet.marketai.engine;

import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.MarketBar;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-process pub/sub for bar and signal updates.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketEventPublisher {

    private final List<Consumer<MarketBar>> barListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<AiSignal>> signalListeners = new CopyOnWriteArrayList<>();

    public AutoCloseable onBar(Consumer<MarketBar> listener) {
        barListeners.add(listener);
        return () -> barListeners.remove(listener);
    }

    public AutoCloseable onSignal(Consumer<AiSignal> listener) {
        signalListeners.add(listener);
        return () -> signalListeners.remove(listener);
    }

    public void publishBar(MarketBar bar) {
        for (Consumer<MarketBar> listener : barListeners) {
            listener.accept(bar);
        }
    }

    public void publishSignal(AiSignal signal) {
        for (Consumer<AiSignal> listener : signalListeners) {
            listener.accept(signal);
        }
    }
}
