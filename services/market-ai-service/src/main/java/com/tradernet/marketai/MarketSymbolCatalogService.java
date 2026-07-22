package com.tradernet.marketai;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Owns a cache-first catalog of exchange-supported symbols.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketSymbolCatalogService implements MarketSymbolProvider, MarketSymbolRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(MarketSymbolCatalogService.class);
    private static final long CACHE_TTL_MS = Duration.ofMinutes(15).toMillis();
    private static final long RETRY_DELAY_MS = Duration.ofMinutes(1).toMillis();

    @EJB
    private BinanceMarketDataClient marketDataClient;

    @Resource
    private SessionContext sessionContext;

    private final AtomicBoolean refreshInFlight = new AtomicBoolean();
    private volatile List<String> cachedSymbols = List.of("BTCUSDT");
    private volatile long cachedSymbolsAtMs;
    private volatile long lastRefreshAttemptAtMs;

    @Override
    public List<String> getSupportedSymbols(String quoteCurrency) {
        requestRefreshIfStale();
        final List<String> snapshot = cachedSymbols;
        final String normalizedQuoteCurrency = MarketSymbolNormalizer.normalizeQuoteCurrency(quoteCurrency);
        final List<String> filtered = snapshot.stream()
            .filter(symbol -> symbol.endsWith(normalizedQuoteCurrency))
            .collect(Collectors.toList());
        if (!filtered.isEmpty()) {
            return filtered;
        }

        final List<String> usdTFallback = snapshot.stream()
            .filter(symbol -> symbol.endsWith("USDT"))
            .collect(Collectors.toList());
        return usdTFallback.isEmpty() ? List.of("BTCUSDT") : usdTFallback;
    }

    @Override
    @Asynchronous
    public void refreshSymbols() {
        try {
            final List<String> remoteSymbols = marketDataClient.fetchExchangeSymbols();
            if (!remoteSymbols.isEmpty()) {
                cachedSymbols = List.copyOf(remoteSymbols);
                cachedSymbolsAtMs = System.currentTimeMillis();
            }
        } catch (RuntimeException ex) {
            LOG.warn("Unable to refresh the market symbol catalog.", ex);
        } finally {
            refreshInFlight.set(false);
        }
    }

    private void requestRefreshIfStale() {
        final long now = System.currentTimeMillis();
        if (now - cachedSymbolsAtMs <= CACHE_TTL_MS
            || now - lastRefreshAttemptAtMs <= RETRY_DELAY_MS
            || !refreshInFlight.compareAndSet(false, true)) {
            return;
        }

        lastRefreshAttemptAtMs = now;
        try {
            sessionContext.getBusinessObject(MarketSymbolRefreshService.class).refreshSymbols();
        } catch (RuntimeException ex) {
            refreshInFlight.set(false);
            LOG.warn("Unable to schedule a market symbol catalog refresh.", ex);
        }
    }
}
