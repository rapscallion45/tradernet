package com.tradernet.marketai.orderbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.MarketAiConfiguration;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the lifecycle of per-symbol order-book clients.
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketOrderBookService {

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, BinanceOrderBookClient> clientsBySymbol = new ConcurrentHashMap<>();
    private final Set<String> startsInFlight = ConcurrentHashMap.newKeySet();
    private final Set<String> syncsInFlight = ConcurrentHashMap.newKeySet();

    @Resource
    private SessionContext sessionContext;

    @jakarta.ejb.EJB
    private MarketAiConfiguration configuration;

    @Lock(LockType.READ)
    public OrderBookSnapshot getOrderBook(String symbol, int levels) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final BinanceOrderBookClient client = clientFor(normalizedSymbol);
        requestStart(normalizedSymbol);
        if (client.shouldRetrySync()) {
            requestSync(sessionContext.getBusinessObject(MarketOrderBookService.class), normalizedSymbol);
        }
        return client.getSnapshot(levels);
    }

    @Asynchronous
    @Lock(LockType.READ)
    public void startOrderBook(String normalizedSymbol) {
        try {
            clientFor(normalizedSymbol).ensureStarted();
        } finally {
            startsInFlight.remove(normalizedSymbol);
        }
    }

    @Asynchronous
    @Lock(LockType.READ)
    public void retryOrderBookSync(String normalizedSymbol) {
        try {
            clientFor(normalizedSymbol).retrySync();
        } finally {
            syncsInFlight.remove(normalizedSymbol);
        }
    }

    @PreDestroy
    @Lock(LockType.WRITE)
    public void stop() {
        clientsBySymbol.values().forEach(BinanceOrderBookClient::stop);
        clientsBySymbol.clear();
        startsInFlight.clear();
        syncsInFlight.clear();
    }

    private BinanceOrderBookClient clientFor(String normalizedSymbol) {
        final MarketOrderBookService service = sessionContext.getBusinessObject(MarketOrderBookService.class);
        return clientsBySymbol.computeIfAbsent(
            normalizedSymbol,
            key -> new BinanceOrderBookClient(
                key,
                httpClient,
                objectMapper,
                reason -> requestSync(service, key),
                configuration.getBinanceRestBaseUrl(),
                configuration.getBinanceWebSocketBaseUrl(),
                configuration.getOrderBookSnapshotLimit(),
                configuration.getOrderBookStaleAfterMs()
            )
        );
    }

    private void requestStart(String normalizedSymbol) {
        if (!startsInFlight.add(normalizedSymbol)) {
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
}
