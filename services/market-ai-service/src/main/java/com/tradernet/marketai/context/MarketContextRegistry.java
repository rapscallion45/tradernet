package com.tradernet.marketai.context;

import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.MarketContextSnapshot;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory bridge between scheduled market-data ingestion and real-time feature/scoring code.
 */
public class MarketContextRegistry {

    private final Map<String, MarketContextSnapshot> snapshotsBySymbol = new ConcurrentHashMap<>();

    public void update(String symbol, MarketContextSnapshot snapshot) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }
        snapshot.setAvailable(true);
        snapshotsBySymbol.put(normalize(symbol), snapshot);
    }

    public MarketContextSnapshot get(String symbol) {
        return snapshotsBySymbol.getOrDefault(normalize(symbol), MarketContextSnapshot.neutral());
    }

    public Set<String> symbols() {
        return Set.copyOf(snapshotsBySymbol.keySet());
    }

    public FeatureSnapshot enrich(FeatureSnapshot features) {
        return new FeatureSnapshot(
                features.getSymbol(),
                features.getEventTime(),
                features.getClose(),
                features.getEmaFast(),
                features.getEmaSlow(),
                features.getRsi(),
                get(features.getSymbol())
        );
    }

    private String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
    }
}
