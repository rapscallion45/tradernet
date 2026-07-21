package com.tradernet.marketai.context;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.MarketAiConfiguration;
import com.tradernet.marketai.model.MarketContextSnapshot;
import com.tradernet.marketai.model.MarketContextUpdateRequest;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.SessionContext;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns market-context registration, hydration and manual updates.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketContextService {

    private final MarketContextRegistry marketContextRegistry = new MarketContextRegistry();
    private final Set<String> contextRefreshSymbols = ConcurrentHashMap.newKeySet();
    private final Set<String> refreshesInFlight = ConcurrentHashMap.newKeySet();

    @EJB
    private MarketContextDataIngestionClient contextDataIngestionClient;

    @EJB
    private MarketAiConfiguration configuration;

    @Resource
    private SessionContext sessionContext;

    public MarketContextRegistry registry() {
        return marketContextRegistry;
    }

    public void registerSymbol(String symbol) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        if (!normalizedSymbol.isBlank()) {
            contextRefreshSymbols.add(normalizedSymbol);
        }
    }

    public void registerSymbols(String symbols) {
        if (symbols == null || symbols.isBlank()) {
            return;
        }

        for (String symbol : symbols.split(",")) {
            registerSymbol(symbol);
        }
    }

    public MarketContextSnapshot get(String symbol) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        registerSymbol(normalizedSymbol);
        final MarketContextSnapshot snapshot = marketContextRegistry.get(normalizedSymbol);
        if (!snapshot.isAvailable()) {
            requestHydration(normalizedSymbol);
        }
        return snapshot;
    }

    public void refresh() {
        if (!configuration.isContextIngestionEnabled()) {
            return;
        }

        contextRefreshSymbols.addAll(marketContextRegistry.symbols());
        contextRefreshSymbols.forEach(this::requestHydration);
    }

    public void update(String symbol, MarketContextUpdateRequest request) {
        if (request == null) {
            throw new InvalidMarketContextException("market context update request is required");
        }
        if (!request.hasAnyUpdate()) {
            throw new InvalidMarketContextException("at least one market context input is required");
        }
        if (!request.hasOnlyFiniteValues()) {
            throw new InvalidMarketContextException("market context inputs must be finite numbers");
        }

        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        registerSymbol(normalizedSymbol);
        final MarketContextSnapshot current = marketContextRegistry.get(normalizedSymbol);
        marketContextRegistry.update(normalizedSymbol, request.toSnapshot(current));
    }

    @Asynchronous
    public void hydrateAsync(String symbol) {
        try {
            hydrate(symbol);
        } finally {
            refreshesInFlight.remove(symbol);
        }
    }

    private void requestHydration(String symbol) {
        if (!configuration.isContextIngestionEnabled() || symbol == null || symbol.isBlank()
            || !refreshesInFlight.add(symbol)) {
            return;
        }

        try {
            sessionContext.getBusinessObject(MarketContextService.class).hydrateAsync(symbol);
        } catch (RuntimeException ex) {
            refreshesInFlight.remove(symbol);
        }
    }

    private void hydrate(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }

        final MarketContextSnapshot current = marketContextRegistry.get(symbol);
        final MarketContextSnapshot hydrated = contextDataIngestionClient.fetch(symbol, current);
        if (hydrated.isAvailable()) {
            marketContextRegistry.update(symbol, hydrated);
        }
    }
}
