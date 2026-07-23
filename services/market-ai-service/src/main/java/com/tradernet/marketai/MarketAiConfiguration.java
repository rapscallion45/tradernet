package com.tradernet.marketai;

import com.tradernet.marketai.scoring.SignalScoringSettings;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;

/**
 * Validates and exposes market-service runtime configuration from one boundary.
 */
@Singleton
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketAiConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MarketAiConfiguration.class);

    private String defaultSymbol;
    private String contextSymbols;
    private boolean contextIngestionEnabled;
    private boolean signalBullScoreEnabled;
    private long signalBullScoreTtlMs;
    private int signalBullScoreHorizonDays;
    private long forecastTtlMs;
    private String binanceRestBaseUrl;
    private String binanceWebSocketBaseUrl;
    private URI forecastingBaseUri;
    private URI ollamaBaseUri;
    private String ollamaModel;
    private boolean ollamaEnabled;
    private int orderBookSnapshotLimit;
    private long orderBookStaleAfterMs;
    private int maxOrderBookSymbols;
    private long orderBookIdleTimeoutMs;
    private int maxLiveSymbols;
    private SignalScoringSettings scoringSettings;

    @PostConstruct
    void load() {
        defaultSymbol = stringValue("market.ai.symbol", "btcusdt");
        contextSymbols = stringValue("market.ai.context.symbols", defaultSymbol);
        contextIngestionEnabled = booleanValue("market.ai.context.ingestion.enabled", true);
        signalBullScoreEnabled = booleanValue("market.ai.signalBullScore.enabled", true);
        signalBullScoreTtlMs = longValue(
            "market.ai.signalBullScoreTtlMs",
            Duration.ofMinutes(1).toMillis(),
            1L,
            Duration.ofHours(1).toMillis()
        );
        signalBullScoreHorizonDays = intValue("market.ai.signalBullScoreHorizonDays", 1, 1, 365);
        forecastTtlMs = longValue(
            "market.ai.forecast.ttlMs",
            Duration.ofMinutes(1).toMillis(),
            1L,
            Duration.ofHours(1).toMillis()
        );
        binanceRestBaseUrl = baseUrl("market.ai.binance.restBaseUrl", "https://api.binance.com");
        binanceWebSocketBaseUrl = baseUrl(
            "market.ai.binance.wsBaseUrl",
            "wss://stream.binance.com:9443/ws"
        );
        forecastingBaseUri = uriValue("market.ai.forecasting.url", "http://forecasting-service:8000");
        ollamaBaseUri = uriValue("market.ai.ollama.url", "http://ollama:11434");
        ollamaModel = stringValue("market.ai.ollama.model", "gemma4:e4b");
        ollamaEnabled = booleanValue("market.ai.ollama.enabled", true);
        orderBookSnapshotLimit = intValue("market.ai.orderBook.snapshotLimit", 5_000, 5, 5_000);
        orderBookStaleAfterMs = longValue(
            "market.ai.orderBook.staleAfterMs",
            Duration.ofSeconds(30).toMillis(),
            1_000L,
            Duration.ofMinutes(10).toMillis()
        );
        maxOrderBookSymbols = intValue("market.ai.orderBook.maxSymbols", 32, 1, 500);
        orderBookIdleTimeoutMs = longValue(
            "market.ai.orderBook.idleTimeoutMs",
            Duration.ofMinutes(5).toMillis(),
            Duration.ofMinutes(1).toMillis(),
            Duration.ofHours(1).toMillis()
        );
        maxLiveSymbols = intValue("market.ai.live.maxSymbols", 32, 1, 500);
        scoringSettings = new SignalScoringSettings(
            stringValue("market.ai.scorer", "context").toLowerCase(Locale.ROOT),
            doubleValue("market.ai.model.buyThreshold", 0.56, 0.5, 1.0),
            doubleValue("market.ai.model.sellThreshold", 0.44, 0.0, 0.5),
            intValue("market.ai.context.buyScoreThreshold", 54, 50, 100),
            intValue("market.ai.context.sellScoreThreshold", 46, 0, 50),
            intValue("market.ai.context.buyExtremeThreshold", 64, 50, 100),
            intValue("market.ai.context.sellExtremeThreshold", 36, 0, 50),
            doubleValue("market.ai.context.forecastWeight", 0.45, 0.0, 1.0),
            doubleValue("market.ai.context.forecastNeutralBand", 8.0, 0.0, 50.0)
        );
    }

    public String getDefaultSymbol() {
        return defaultSymbol;
    }

    public String getContextSymbols() {
        return contextSymbols;
    }

    public boolean isContextIngestionEnabled() {
        return contextIngestionEnabled;
    }

    public boolean isSignalBullScoreEnabled() {
        return signalBullScoreEnabled;
    }

    public long getSignalBullScoreTtlMs() {
        return signalBullScoreTtlMs;
    }

    public int getSignalBullScoreHorizonDays() {
        return signalBullScoreHorizonDays;
    }

    public long getForecastTtlMs() {
        return forecastTtlMs;
    }

    public String getBinanceRestBaseUrl() {
        return binanceRestBaseUrl;
    }

    public String getBinanceWebSocketBaseUrl() {
        return binanceWebSocketBaseUrl;
    }

    public URI getForecastingBaseUri() {
        return forecastingBaseUri;
    }

    public URI getOllamaBaseUri() {
        return ollamaBaseUri;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public boolean isOllamaEnabled() {
        return ollamaEnabled;
    }

    public int getOrderBookSnapshotLimit() {
        return orderBookSnapshotLimit;
    }

    public long getOrderBookStaleAfterMs() {
        return orderBookStaleAfterMs;
    }

    public int getMaxOrderBookSymbols() {
        return maxOrderBookSymbols;
    }

    public long getOrderBookIdleTimeoutMs() {
        return orderBookIdleTimeoutMs;
    }

    public int getMaxLiveSymbols() {
        return maxLiveSymbols;
    }

    public SignalScoringSettings getScoringSettings() {
        return scoringSettings;
    }

    private String stringValue(String key, String fallback) {
        final String value = System.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean booleanValue(String key, boolean fallback) {
        final String value = System.getProperty(key);
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value.trim());
    }

    private int intValue(String key, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(stringValue(key, String.valueOf(fallback)))));
        } catch (NumberFormatException ex) {
            LOG.warn("Invalid integer configuration for {}; using {}.", key, fallback);
            return fallback;
        }
    }

    private long longValue(String key, long fallback, long min, long max) {
        try {
            return Math.max(min, Math.min(max, Long.parseLong(stringValue(key, String.valueOf(fallback)))));
        } catch (NumberFormatException ex) {
            LOG.warn("Invalid long configuration for {}; using {}.", key, fallback);
            return fallback;
        }
    }

    private double doubleValue(String key, double fallback, double min, double max) {
        try {
            return Math.max(min, Math.min(max, Double.parseDouble(stringValue(key, String.valueOf(fallback)))));
        } catch (NumberFormatException ex) {
            LOG.warn("Invalid decimal configuration for {}; using {}.", key, fallback);
            return fallback;
        }
    }

    private String baseUrl(String key, String fallback) {
        final String value = stringValue(key, fallback);
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private URI uriValue(String key, String fallback) {
        try {
            return URI.create(stringValue(key, fallback));
        } catch (IllegalArgumentException ex) {
            LOG.warn("Invalid URI configuration for {}; using {}.", key, fallback);
            return URI.create(fallback);
        }
    }
}
