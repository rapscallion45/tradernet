package com.tradernet.user;

import com.tradernet.user.dto.LoginStatus;

/**
 * Service-layer outcome for login attempts.
 */
public class AuthenticationResult {

    private final LoginStatus status;
    private final String sessionToken;
    private final String passwordResetToken;

    private AuthenticationResult(LoginStatus status, String sessionToken, String passwordResetToken) {
        this.status = status;
        this.sessionToken = sessionToken;
        this.passwordResetToken = passwordResetToken;
    }

    public static AuthenticationResult status(LoginStatus status) {
        return new AuthenticationResult(status, null, null);
    }

    public static AuthenticationResult success(String sessionToken) {
        return new AuthenticationResult(LoginStatus.SUCCESS, sessionToken, null);
    }

    public static AuthenticationResult passwordExpired(String passwordResetToken) {
        return new AuthenticationResult(LoginStatus.ACCOUNT_PASSWORD_EXPIRED, null, passwordResetToken);
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
}
