package com.tradernet.marketai.orderbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.MarketAiConfiguration;
import com.tradernet.marketai.MarketDataCapacityException;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns a bounded, idle-evicted pool of per-symbol order-book clients.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketOrderBookService {

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, OrderBookRuntime> runtimesBySymbol = new ConcurrentHashMap<>();
    private final Set<String> startsInFlight = ConcurrentHashMap.newKeySet();
    private final Set<String> syncsInFlight = ConcurrentHashMap.newKeySet();

    @Resource
    private SessionContext sessionContext;

    @EJB
    private MarketAiConfiguration configuration;

    public OrderBookSnapshot getOrderBook(String symbol, int levels) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final OrderBookRuntime runtime;
        synchronized (runtimesBySymbol) {
            OrderBookRuntime existing = runtimesBySymbol.get(normalizedSymbol);
            if (existing == null) {
                if (runtimesBySymbol.size() >= configuration.getMaxOrderBookSymbols()) {
                    throw new MarketDataCapacityException("Order-book capacity is currently exhausted");
                }
                existing = new OrderBookRuntime(createClient(normalizedSymbol));
                runtimesBySymbol.put(normalizedSymbol, existing);
            }
            existing.lastAccessAtMs = System.currentTimeMillis();
            runtime = existing;
        }
        requestStart(normalizedSymbol);
        if (runtime.client.shouldRetrySync()) {
            requestSync(sessionContext.getBusinessObject(MarketOrderBookService.class), normalizedSymbol);
        }
        return runtime.client.getSnapshot(levels);
    }

    @Asynchronous
    public void startOrderBook(String normalizedSymbol) {
        try {
            final OrderBookRuntime runtime = runtimesBySymbol.get(normalizedSymbol);
            if (runtime != null) {
                runtime.client.ensureStarted();
            }
        } finally {
            startsInFlight.remove(normalizedSymbol);
        }
    }

    @Asynchronous
    public void retryOrderBookSync(String normalizedSymbol) {
        try {
            final OrderBookRuntime runtime = runtimesBySymbol.get(normalizedSymbol);
            if (runtime != null) {
                runtime.client.retrySync();
            }
        } finally {
            syncsInFlight.remove(normalizedSymbol);
        }
    }

    @Schedule(hour = "*", minute = "*", second = "0", persistent = false)
    public void evictIdleClients() {
        final long cutoff = System.currentTimeMillis() - configuration.getOrderBookIdleTimeoutMs();
        final List<OrderBookRuntime> evicted = new ArrayList<>();
        synchronized (runtimesBySymbol) {
            final Iterator<Map.Entry<String, OrderBookRuntime>> entries = runtimesBySymbol.entrySet().iterator();
            while (entries.hasNext()) {
                final Map.Entry<String, OrderBookRuntime> entry = entries.next();
                if (entry.getValue().lastAccessAtMs >= cutoff
                    || startsInFlight.contains(entry.getKey())
                    || syncsInFlight.contains(entry.getKey())) {
                    continue;
                }
                entries.remove();
                startsInFlight.remove(entry.getKey());
                syncsInFlight.remove(entry.getKey());
                evicted.add(entry.getValue());
            }
        }
        evicted.forEach(runtime -> runtime.client.stop());
    }

    @PreDestroy
    public void stop() {
        final List<OrderBookRuntime> runtimes;
        synchronized (runtimesBySymbol) {
            runtimes = new ArrayList<>(runtimesBySymbol.values());
            runtimesBySymbol.clear();
        }
        runtimes.forEach(runtime -> runtime.client.stop());
        startsInFlight.clear();
        syncsInFlight.clear();
    }

    private BinanceOrderBookClient createClient(String normalizedSymbol) {
        final MarketOrderBookService service = sessionContext.getBusinessObject(MarketOrderBookService.class);
        return new BinanceOrderBookClient(
            normalizedSymbol,
            httpClient,
            objectMapper,
            reason -> requestSync(service, normalizedSymbol),
            configuration.getBinanceRestBaseUrl(),
            configuration.getBinanceWebSocketBaseUrl(),
            configuration.getOrderBookSnapshotLimit(),
            configuration.getOrderBookStaleAfterMs()
        );
    }

    private void requestStart(String normalizedSymbol) {
        if (!runtimesBySymbol.containsKey(normalizedSymbol) || !startsInFlight.add(normalizedSymbol)) {
            return;
        }
        try {
            sessionContext.getBusinessObject(MarketOrderBookService.class).startOrderBook(normalizedSymbol);
        } catch (RuntimeException ex) {
            startsInFlight.remove(normalizedSymbol);
        }
    }

    private void requestSync(MarketOrderBookService service, String normalizedSymbol) {
        if (!syncsInFlight.add(normalizedSymbol)) {
            return;
        }
        try {
            service.retryOrderBookSync(normalizedSymbol);
        } catch (RuntimeException ex) {
            syncsInFlight.remove(normalizedSymbol);
        }
    }

    private static final class OrderBookRuntime {
        private final BinanceOrderBookClient client;
        private volatile long lastAccessAtMs = System.currentTimeMillis();

        private OrderBookRuntime(BinanceOrderBookClient client) {
            this.client = client;
        }
    }
}
