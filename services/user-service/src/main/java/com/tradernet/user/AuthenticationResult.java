package com.tradernet.user;

import com.tradernet.user.dto.LoginStatus;

/**
 * Service-layer outcome for login attempts.
 */
public class AuthenticationResult {

    private final LoginStatus status;
    private final String sessionToken;
    private final String passwordResetToken;
    private final long retryAfterSeconds;

    private AuthenticationResult(
        LoginStatus status,
        String sessionToken,
        String passwordResetToken,
        long retryAfterSeconds
    ) {
        this.status = status;
        this.sessionToken = sessionToken;
        this.passwordResetToken = passwordResetToken;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static AuthenticationResult status(LoginStatus status) {
        return new AuthenticationResult(status, null, null, 0);
    }

    public static AuthenticationResult success(String sessionToken) {
        return new AuthenticationResult(LoginStatus.SUCCESS, sessionToken, null, 0);
    }

    public static AuthenticationResult passwordExpired(String passwordResetToken) {
        return new AuthenticationResult(LoginStatus.ACCOUNT_PASSWORD_EXPIRED, null, passwordResetToken, 0);
    }

    public static AuthenticationResult rateLimited(long retryAfterSeconds) {
        return new AuthenticationResult(LoginStatus.RATE_LIMITED, null, null, retryAfterSeconds);
    }

    public LoginStatus getStatus() {
        return status;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
