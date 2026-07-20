package com.tradernet.marketai.context;

import com.tradernet.marketai.MarketSymbolNormalizer;
import com.tradernet.marketai.model.MarketContextSnapshot;
import com.tradernet.marketai.model.MarketContextUpdateRequest;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
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

    @EJB
    private MarketContextDataIngestionClient contextDataIngestionClient;

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
        return getHydrated(symbol);
    }

    public MarketContextSnapshot getHydrated(String symbol) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        registerSymbol(normalizedSymbol);
        MarketContextSnapshot snapshot = marketContextRegistry.get(normalizedSymbol);
        if (!snapshot.isAvailable()) {
            hydrate(normalizedSymbol);
            snapshot = marketContextRegistry.get(normalizedSymbol);
        }
        return snapshot;
    }

    public void refresh() {
        if (!Boolean.parseBoolean(System.getProperty("market.ai.context.ingestion.enabled", "true"))) {
            return;
        }

        contextRefreshSymbols.addAll(marketContextRegistry.symbols());
        contextRefreshSymbols.forEach(this::hydrate);
    }

    public void update(String symbol, MarketContextUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("market context update request is required");
        }
        if (!request.hasAnyUpdate()) {
            throw new IllegalArgumentException("at least one market context input is required");
        }

        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        registerSymbol(normalizedSymbol);
        final MarketContextSnapshot current = marketContextRegistry.get(normalizedSymbol);
        marketContextRegistry.update(normalizedSymbol, request.toSnapshot(current));
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
