package com.tradernet.user;

/**
 * Outcome of a distributed authentication rate-limit check.
 */
public final class RateLimitDecision {

    private static final RateLimitDecision ALLOWED = new RateLimitDecision(true, 0);

    private final boolean allowed;
    private final long retryAfterSeconds;

    private RateLimitDecision(boolean allowed, long retryAfterSeconds) {
        this.allowed = allowed;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static RateLimitDecision allowed() {
        return ALLOWED;
    }

    public static RateLimitDecision denied(long retryAfterSeconds) {
        return new RateLimitDecision(false, Math.max(1, retryAfterSeconds));
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
