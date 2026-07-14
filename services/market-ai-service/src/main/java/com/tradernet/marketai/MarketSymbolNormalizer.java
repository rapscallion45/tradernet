package com.tradernet.marketai;

import java.util.Locale;

/**
 * Normalizes market symbols and quote-currency query values used across market collaborators.
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
        if ("USD".equals(upper)) {
            return "USDT";
        }

        return upper;
    }
}
