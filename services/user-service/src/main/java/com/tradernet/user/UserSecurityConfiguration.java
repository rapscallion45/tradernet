package com.tradernet.user;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.Duration;

/**
 * Validated authentication and bootstrap configuration.
 */
@Singleton
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class UserSecurityConfiguration {

    private static final int DEFAULT_MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int DEFAULT_LOCKOUT_DURATION_SECONDS = 900;
    private static final int DEFAULT_MINIMUM_PASSWORD_LENGTH = 15;
    private static final int DEFAULT_MAXIMUM_PASSWORD_LENGTH = 128;
    private static final int DEFAULT_MAXIMUM_PASSWORD_BYTES = 512;
    private static final int DEFAULT_ARGON2_MEMORY_KIB = 19_456;
    private static final int DEFAULT_ARGON2_ITERATIONS = 2;
    private static final int DEFAULT_ARGON2_PARALLELISM = 1;
    private static final int DEFAULT_LOGIN_RATE_LIMIT_ATTEMPTS = 60;
    private static final int DEFAULT_RESET_RATE_LIMIT_ATTEMPTS = 20;
    private static final int DEFAULT_RATE_LIMIT_WINDOW_SECONDS = 300;
    private static final int DEFAULT_RATE_LIMIT_BLOCK_SECONDS = 900;
    private static final int DEFAULT_SESSION_ABSOLUTE_SECONDS = 28_800;
    private static final int DEFAULT_SESSION_IDLE_SECONDS = 1_800;
    private static final int DEFAULT_PASSWORD_RESET_SECONDS = 600;
    private static final String DEFAULT_BOOTSTRAP_PASSWORD = "changeme";

    private int maxFailedLoginAttempts;
    private Duration lockoutDuration;
    private int minimumPasswordLength;
    private int maximumPasswordLength;
    private int maximumPasswordBytes;
    private int argon2MemoryKiB;
    private int argon2Iterations;
    private int argon2Parallelism;
    private int loginRateLimitAttempts;
    private int resetRateLimitAttempts;
    private Duration rateLimitWindow;
    private Duration rateLimitBlockDuration;
    private Duration sessionAbsoluteDuration;
    private Duration sessionIdleDuration;
    private Duration passwordResetDuration;
    private String passwordBlocklistPath;
    private String bootstrapSuperuserPassword;
    private String bootstrapAdminPassword;
    private String bootstrapStandardPassword;
    private boolean insecureBootstrapPasswordEnabled;

    @PostConstruct
    void load() {
        maxFailedLoginAttempts = intValue(
            "tradernet.auth.maxFailedLoginAttempts",
            DEFAULT_MAX_FAILED_LOGIN_ATTEMPTS,
            1,
            20
        );
        lockoutDuration = Duration.ofSeconds(intValue(
            "tradernet.auth.lockoutDurationSeconds",
            DEFAULT_LOCKOUT_DURATION_SECONDS,
            30,
            86_400
        ));
        minimumPasswordLength = intValue(
            "tradernet.auth.password.minimumLength",
            DEFAULT_MINIMUM_PASSWORD_LENGTH,
            12,
            64
        );
        maximumPasswordLength = intValue(
            "tradernet.auth.password.maximumLength",
            DEFAULT_MAXIMUM_PASSWORD_LENGTH,
            64,
            256
        );
        maximumPasswordBytes = intValue(
            "tradernet.auth.password.maximumBytes",
            DEFAULT_MAXIMUM_PASSWORD_BYTES,
            256,
            2_048
        );
        argon2MemoryKiB = intValue(
            "tradernet.auth.password.argon2.memoryKiB",
            DEFAULT_ARGON2_MEMORY_KIB,
            12_288,
            262_144
        );
        argon2Iterations = intValue(
            "tradernet.auth.password.argon2.iterations",
            DEFAULT_ARGON2_ITERATIONS,
            1,
            10
        );
        argon2Parallelism = intValue(
            "tradernet.auth.password.argon2.parallelism",
            DEFAULT_ARGON2_PARALLELISM,
            1,
            8
        );
        loginRateLimitAttempts = intValue(
            "tradernet.auth.rateLimit.login.maxAttempts",
            DEFAULT_LOGIN_RATE_LIMIT_ATTEMPTS,
            5,
            10_000
        );
        resetRateLimitAttempts = intValue(
            "tradernet.auth.rateLimit.passwordReset.maxAttempts",
            DEFAULT_RESET_RATE_LIMIT_ATTEMPTS,
            3,
            1_000
        );
        rateLimitWindow = Duration.ofSeconds(intValue(
            "tradernet.auth.rateLimit.windowSeconds",
            DEFAULT_RATE_LIMIT_WINDOW_SECONDS,
            10,
            86_400
        ));
        rateLimitBlockDuration = Duration.ofSeconds(intValue(
            "tradernet.auth.rateLimit.blockSeconds",
            DEFAULT_RATE_LIMIT_BLOCK_SECONDS,
            30,
            86_400
        ));
        sessionAbsoluteDuration = Duration.ofSeconds(intValue(
            "tradernet.auth.session.absoluteSeconds",
            DEFAULT_SESSION_ABSOLUTE_SECONDS,
            300,
            86_400
        ));
        sessionIdleDuration = Duration.ofSeconds(intValue(
            "tradernet.auth.session.idleSeconds",
            DEFAULT_SESSION_IDLE_SECONDS,
            60,
            14_400
        ));
        if (sessionIdleDuration.compareTo(sessionAbsoluteDuration) > 0) {
            throw new IllegalStateException("tradernet.auth.session.idleSeconds must not exceed absoluteSeconds");
        }
        passwordResetDuration = Duration.ofSeconds(intValue(
            "tradernet.auth.passwordReset.durationSeconds",
            DEFAULT_PASSWORD_RESET_SECONDS,
            120,
            3_600
        ));
        passwordBlocklistPath = firstNonBlank(
            System.getProperty("tradernet.auth.password.blocklistPath"),
            System.getenv("TRADERNET_AUTH_PASSWORD_BLOCKLIST_PATH")
        );
        bootstrapSuperuserPassword = firstNonBlank(
            System.getProperty("tradernet.bootstrap.superuserPassword"),
            System.getenv("TRADERNET_BOOTSTRAP_SUPERUSER_PASSWORD")
        );
        bootstrapAdminPassword = firstNonBlank(
            System.getProperty("tradernet.bootstrap.adminPassword"),
            System.getenv("TRADERNET_BOOTSTRAP_ADMIN_PASSWORD")
        );
        bootstrapStandardPassword = firstNonBlank(
            System.getProperty("tradernet.bootstrap.standardPassword"),
            System.getenv("TRADERNET_BOOTSTRAP_STANDARD_PASSWORD")
        );
        insecureBootstrapPasswordEnabled = booleanValue(
            "tradernet.bootstrap.allowDefaultPassword",
            System.getProperty("tradernet.bootstrap.allowDefaultPassword"),
            System.getenv("TRADERNET_BOOTSTRAP_ALLOW_DEFAULT_PASSWORD"),
            false
        );
    }

    public int getMaxFailedLoginAttempts() {
        return maxFailedLoginAttempts;
    }

    public Duration getLockoutDuration() {
        return lockoutDuration;
    }

    public int getMinimumPasswordLength() {
        return minimumPasswordLength;
    }

    public int getMaximumPasswordBytes() {
        return maximumPasswordBytes;
    }

    public int getMaximumPasswordLength() {
        return maximumPasswordLength;
    }

    public int getArgon2MemoryKiB() {
        return argon2MemoryKiB;
    }

    public int getArgon2Iterations() {
        return argon2Iterations;
    }

    public int getArgon2Parallelism() {
        return argon2Parallelism;
    }

    public int getLoginRateLimitAttempts() {
        return loginRateLimitAttempts;
    }

    public int getResetRateLimitAttempts() {
        return resetRateLimitAttempts;
    }

    public Duration getRateLimitWindow() {
        return rateLimitWindow;
    }

    public Duration getRateLimitBlockDuration() {
        return rateLimitBlockDuration;
    }

    public Duration getSessionAbsoluteDuration() {
        return sessionAbsoluteDuration;
    }

    public Duration getSessionIdleDuration() {
        return sessionIdleDuration;
    }

    public Duration getPasswordResetDuration() {
        return passwordResetDuration;
    }

    public String getPasswordBlocklistPath() {
        return passwordBlocklistPath;
    }

    public String getBootstrapPassword(String username) {
        final String configured = configuredBootstrapPassword(username);
        return configured != null || !insecureBootstrapPasswordEnabled
            ? configured
            : DEFAULT_BOOTSTRAP_PASSWORD;
    }

    public boolean isInsecureBootstrapPasswordEnabled() {
        return insecureBootstrapPasswordEnabled;
    }

    public boolean isUsingInsecureBootstrapPassword(String username) {
        return insecureBootstrapPasswordEnabled && configuredBootstrapPassword(username) == null;
    }

    String getInsecureBootstrapPassword() {
        return DEFAULT_BOOTSTRAP_PASSWORD;
    }

    private String configuredBootstrapPassword(String username) {
        if ("superuser".equals(username)) {
            return bootstrapSuperuserPassword;
        }
        if ("admin".equals(username)) {
            return bootstrapAdminPassword;
        }
        if ("standard".equals(username)) {
            return bootstrapStandardPassword;
        }
        return null;
    }

    private int intValue(String key, int fallback, int min, int max) {
        final String configured = System.getProperty(key);
        if (configured == null || configured.isBlank()) {
            return fallback;
        }
        try {
            final int value = Integer.parseInt(configured.trim());
            if (value < min || value > max) {
                throw new IllegalStateException(key + " must be between " + min + " and " + max);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(key + " must be an integer", ex);
        }
    }

    private boolean booleanValue(String key, String propertyValue, String environmentValue, boolean fallback) {
        final String configured = firstNonBlank(propertyValue, environmentValue);
        if (configured == null) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(configured)) {
            return true;
        }
        if ("false".equalsIgnoreCase(configured)) {
            return false;
        }
        throw new IllegalStateException(key + " must be true or false");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
