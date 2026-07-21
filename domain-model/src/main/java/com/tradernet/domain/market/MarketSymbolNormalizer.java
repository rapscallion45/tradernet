package com.tradernet.domain.market;

import java.util.Locale;

/**
 * Canonical normalization for market symbols and quote currencies shared across bounded contexts.
 */
public final class MarketSymbolNormalizer {

    private MarketSymbolNormalizer() {
    }

    public static String normalizeSymbol(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            return "BTCUSDT";
        }

        final String upper = rawSymbol.trim().toUpperCase(Locale.ROOT);
        if ("BTCUSD".equals(upper)) {
            return "BTCUSDT";
        }
        if ("ETHUSD".equals(upper)) {
            return "ETHUSDT";
        }
        return upper;
    }

    public static String normalizeQuoteCurrency(String rawCurrency) {
        if (rawCurrency == null || rawCurrency.isBlank()) {
            return "USDT";
        }

        final String upper = rawCurrency.trim().toUpperCase(Locale.ROOT);
        return "USD".equals(upper) ? "USDT" : upper;
    }
}
