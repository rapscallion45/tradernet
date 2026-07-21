package com.tradernet.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiConfigurationTest {

    @AfterEach
    void clearConfiguration() {
        System.clearProperty("tradernet.auth.cookie.secure");
        System.clearProperty("market.ai.websocket.maxPendingEvents");
        System.clearProperty("tradernet.auth.websocket.allowedOrigins");
    }

    @Test
    void secureCookiesAreEnabledByDefault() {
        final ApiConfiguration configuration = new ApiConfiguration();

        configuration.load();

        assertTrue(configuration.useSecureCookies());
    }

    @Test
    void localHttpCanBeEnabledExplicitly() {
        System.setProperty("tradernet.auth.cookie.secure", "false");
        final ApiConfiguration configuration = new ApiConfiguration();

        configuration.load();

        assertFalse(configuration.useSecureCookies());
    }

    @Test
    void malformedSecurityConfigurationFailsStartup() {
        System.setProperty("tradernet.auth.cookie.secure", "yes");

        assertThrows(IllegalStateException.class, () -> new ApiConfiguration().load());
    }

    @Test
    void webSocketsRequireTheSameOriginByDefault() {
        final ApiConfiguration configuration = new ApiConfiguration();
        configuration.load();

        assertTrue(configuration.isWebSocketOriginAllowed(
            "https://app.example.com",
            "wss://app.example.com/api/ws/market",
            "app.example.com"
        ));
        assertFalse(configuration.isWebSocketOriginAllowed(
            "https://attacker.example",
            "wss://app.example.com/api/ws/market",
            "app.example.com"
        ));
        assertFalse(configuration.isWebSocketOriginAllowed(
            null,
            "wss://app.example.com/api/ws/market",
            "app.example.com"
        ));
    }

    @Test
    void configuredWebSocketOriginAllowlistSupportsTrustedProxies() {
        System.setProperty(
            "tradernet.auth.websocket.allowedOrigins",
            "https://trade.example.com, https://admin.example.com"
        );
        final ApiConfiguration configuration = new ApiConfiguration();
        configuration.load();

        assertTrue(configuration.isWebSocketOriginAllowed(
            "https://trade.example.com",
            "/api/ws/market",
            "internal:8080"
        ));
        assertFalse(configuration.isWebSocketOriginAllowed(
            "https://other.example.com",
            "/api/ws/market",
            "internal:8080"
        ));
    }

    @Test
    void malformedWebSocketOriginFailsStartup() {
        System.setProperty("tradernet.auth.websocket.allowedOrigins", "https://example.com/path");

        assertThrows(IllegalStateException.class, () -> new ApiConfiguration().load());
    }
}
