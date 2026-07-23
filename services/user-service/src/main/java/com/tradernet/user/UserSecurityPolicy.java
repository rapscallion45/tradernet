package com.tradernet.user;

import jakarta.ejb.Local;

import java.time.Duration;

/**
 * Typed contract for authentication and password security settings.
 */
@Local
public interface UserSecurityPolicy {

    int getMaxFailedLoginAttempts();

    Duration getLockoutDuration();

    int getMinimumPasswordLength();

    int getMaximumPasswordBytes();

    int getMaximumPasswordLength();

    int getArgon2MemoryKiB();

    int getArgon2Iterations();

    int getArgon2Parallelism();

    int getLoginRateLimitAttempts();

    int getResetRateLimitAttempts();

    Duration getRateLimitWindow();

    Duration getRateLimitBlockDuration();

    Duration getSessionAbsoluteDuration();

    Duration getSessionIdleDuration();

    Duration getPasswordResetDuration();

    String getPasswordBlocklistPath();

    String getBootstrapPassword(String username);

    boolean isInsecureBootstrapPasswordEnabled();

    boolean isUsingInsecureBootstrapPassword(String username);
}
