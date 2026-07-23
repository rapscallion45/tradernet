package com.tradernet.marketai;

import com.tradernet.marketai.engine.MarketEventPublisher;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Atomically owns live pipeline references and symbol-keyed event registrations.
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LiveMarketSubscriptionManager implements LiveMarketSubscriptionService {

    @EJB
    private LiveMarketPipelineService liveMarketPipelineService;

    @EJB
    private MarketEventPublisher eventPublisher;

    private final Map<String, Registration> registrations = new HashMap<>();

    @Override
    @Lock(LockType.WRITE)
    public String subscribe(String symbol, LiveMarketEventListener listener) {
        Objects.requireNonNull(listener, "live market listener is required");
        final String normalizedSymbol = liveMarketPipelineService.acquireSymbol(symbol);
        AutoCloseable barRegistration = null;
        AutoCloseable signalRegistration = null;
        try {
            barRegistration = eventPublisher.onBar(normalizedSymbol, listener::onBar);
            signalRegistration = eventPublisher.onSignal(normalizedSymbol, listener::onSignal);
            final String subscriptionId = UUID.randomUUID().toString();
            registrations.put(
                subscriptionId,
                new Registration(normalizedSymbol, barRegistration, signalRegistration)
            );
            return subscriptionId;
        } catch (RuntimeException ex) {
            closeQuietly(barRegistration);
            closeQuietly(signalRegistration);
            liveMarketPipelineService.releaseSymbol(normalizedSymbol);
            throw ex;
        }
    }

    @Override
    @Lock(LockType.WRITE)
    public void unsubscribe(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return;
        }
        final Registration registration = registrations.remove(subscriptionId);
        if (registration != null) {
            close(registration);
        }
    }

    @PreDestroy
    @Lock(LockType.WRITE)
    public void stop() {
        registrations.values().forEach(this::close);
        registrations.clear();
    }

    private void close(Registration registration) {
        closeQuietly(registration.barRegistration);
        closeQuietly(registration.signalRegistration);
        liveMarketPipelineService.releaseSymbol(registration.symbol);
    }

    private void closeQuietly(AutoCloseable registration) {
        if (registration == null) {
            return;
        }
        try {
            registration.close();
        } catch (Exception ignored) {
            // The registration is already absent.
        }
    }

    private static final class Registration {
        private final String symbol;
        private final AutoCloseable barRegistration;
        private final AutoCloseable signalRegistration;

        private Registration(
            String symbol,
            AutoCloseable barRegistration,
            AutoCloseable signalRegistration
        ) {
            this.symbol = symbol;
            this.barRegistration = barRegistration;
            this.signalRegistration = signalRegistration;
        }
    }
}
