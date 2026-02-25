package com.tradernet.marketai.model;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses chart intervals like 1S, 15D, 10Y and maps them to Binance-supported intervals.
 */
public final class ChartInterval {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("^(\\d+)([A-Z]{1,2})$");

    private static final long SECOND_MS = 1_000L;
    private static final long MINUTE_MS = 60_000L;
    private static final long HOUR_MS = 3_600_000L;
    private static final long DAY_MS = 86_400_000L;
    private static final long MONTH_MS = 30L * DAY_MS;
    private static final long YEAR_MS = 365L * DAY_MS;

    private final String token;
    private final long durationMs;
    private final BinanceBaseInterval baseInterval;
    private final int mergeFactor;

    private ChartInterval(String token, long durationMs, BinanceBaseInterval baseInterval, int mergeFactor) {
        this.token = token;
        this.durationMs = durationMs;
        this.baseInterval = baseInterval;
        this.mergeFactor = mergeFactor;
    }

    public static ChartInterval parse(String rawToken) {
        final String normalized = Optional.ofNullable(rawToken)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .orElse("1S");

        final Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return fromDuration("1S", SECOND_MS);
        }

        final int amount;
        try {
            amount = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return fromDuration("1S", SECOND_MS);
        }

        if (amount <= 0) {
            return fromDuration("1S", SECOND_MS);
        }

        final long multiplier = unitToMs(matcher.group(2));
        final long duration = multiplier * amount;
        return fromDuration(normalized, Math.max(SECOND_MS, duration));
    }

    private static long unitToMs(String unit) {
        switch (unit) {
            case "S":
                return SECOND_MS;
            case "M":
            case "MIN":
                return MINUTE_MS;
            case "H":
                return HOUR_MS;
            case "D":
                return DAY_MS;
            case "N":
            case "MO":
                return MONTH_MS;
            case "Y":
                return YEAR_MS;
            default:
                return SECOND_MS;
        }
    }

    private static ChartInterval fromDuration(String token, long durationMs) {
        BinanceBaseInterval selected = BinanceBaseInterval.ONE_SECOND;
        for (BinanceBaseInterval candidate : BinanceBaseInterval.values()) {
            if (candidate.durationMs <= durationMs) {
                selected = candidate;
            }
        }

        final int factor = Math.max(1, (int) Math.round((double) durationMs / selected.durationMs));
        return new ChartInterval(token, durationMs, selected, factor);
    }

    public String getToken() {
        return token;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getBinanceInterval() {
        return baseInterval.binanceToken;
    }

    public long getBinanceIntervalMs() {
        return baseInterval.durationMs;
    }

    public int getMergeFactor() {
        return mergeFactor;
    }

    private enum BinanceBaseInterval {
        ONE_SECOND("1s", SECOND_MS),
        ONE_MINUTE("1m", MINUTE_MS),
        THREE_MINUTES("3m", 3L * MINUTE_MS),
        FIVE_MINUTES("5m", 5L * MINUTE_MS),
        FIFTEEN_MINUTES("15m", 15L * MINUTE_MS),
        THIRTY_MINUTES("30m", 30L * MINUTE_MS),
        ONE_HOUR("1h", HOUR_MS),
        TWO_HOURS("2h", 2L * HOUR_MS),
        FOUR_HOURS("4h", 4L * HOUR_MS),
        SIX_HOURS("6h", 6L * HOUR_MS),
        EIGHT_HOURS("8h", 8L * HOUR_MS),
        TWELVE_HOURS("12h", 12L * HOUR_MS),
        ONE_DAY("1d", DAY_MS),
        THREE_DAYS("3d", 3L * DAY_MS),
        ONE_WEEK("1w", 7L * DAY_MS),
        ONE_MONTH("1M", MONTH_MS);

        private final String binanceToken;
        private final long durationMs;

        BinanceBaseInterval(String binanceToken, long durationMs) {
            this.binanceToken = binanceToken;
            this.durationMs = durationMs;
        }
    }
}
