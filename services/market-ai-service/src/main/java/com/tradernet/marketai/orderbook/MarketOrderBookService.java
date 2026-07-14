package com.tradernet.marketai.orderbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.marketai.MarketSymbolNormalizer;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
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

    @Lock(LockType.WRITE)
    public OrderBookSnapshot getOrderBook(String symbol, int levels) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        final BinanceOrderBookClient client = clientsBySymbol.computeIfAbsent(
            normalizedSymbol,
            key -> new BinanceOrderBookClient(key, httpClient, objectMapper)
        );
        client.ensureStarted();
        return client.getSnapshot(levels);
    }

    @PreDestroy
    @Lock(LockType.WRITE)
    public void stop() {
        clientsBySymbol.values().forEach(BinanceOrderBookClient::stop);
        clientsBySymbol.clear();
    }
}
