package com.tradernet.user;

/**
 * Service-layer outcome for expired-password reset attempts.
 */
public class PasswordResetResult {

    public enum Status {
        SUCCESS,
        INVALID_REQUEST,
        INVALID_SESSION,
        RATE_LIMITED
    }

    private final Status status;
    private final String message;
    private final long retryAfterSeconds;
    private final long userId;

    private PasswordResetResult(Status status, String message, long retryAfterSeconds, long userId) {
        this.status = status;
        this.message = message;
        this.retryAfterSeconds = retryAfterSeconds;
        this.userId = userId;
    }

    public static PasswordResetResult success(long userId) {
        return new PasswordResetResult(Status.SUCCESS, "Password reset", 0, userId);
    }

    public static PasswordResetResult invalidRequest() {
        return new PasswordResetResult(Status.INVALID_REQUEST, "newPassword is required", 0, 0);
    }

    public static PasswordResetResult invalidSession() {
        return new PasswordResetResult(Status.INVALID_SESSION, "Password reset session is invalid or expired", 0, 0);
    }

    public static PasswordResetResult rateLimited(long retryAfterSeconds) {
        return new PasswordResetResult(Status.RATE_LIMITED, "Too many password reset attempts", retryAfterSeconds, 0);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public long getUserId() {
        return userId;
    }
}
