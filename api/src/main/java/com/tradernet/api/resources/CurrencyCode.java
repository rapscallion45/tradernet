package com.tradernet.api.resources;

import java.util.Locale;

public enum CurrencyCode {
    USD,
    EUR,
    GBP,
    JPY,
    CAD,
    AUD,
    CHF,
    INR,
    BRL,
    MXN,
    CNY,
    KRW,
    SGD,
    HKD,
    ZAR,
    AED;

    public static CurrencyCode parseOrDefault(String raw, CurrencyCode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return CurrencyCode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
