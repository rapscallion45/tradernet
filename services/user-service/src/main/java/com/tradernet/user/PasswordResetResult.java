package com.tradernet.user;

/**
 * Service-layer outcome for expired-password reset attempts.
 */
public class PasswordResetResult {

    public enum Status {
        SUCCESS,
        INVALID_REQUEST,
        INVALID_SESSION,
        USER_NOT_FOUND
    }

    private final Status status;
    private final String message;

    private PasswordResetResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static PasswordResetResult success() {
        return new PasswordResetResult(Status.SUCCESS, "Password reset");
    }

    public static PasswordResetResult invalidRequest() {
        return new PasswordResetResult(Status.INVALID_REQUEST, "username and newPassword are required");
    }

    public static PasswordResetResult invalidSession() {
        return new PasswordResetResult(Status.INVALID_SESSION, "Password reset session is invalid or expired");
    }

    public static PasswordResetResult userNotFound(String message) {
        return new PasswordResetResult(Status.USER_NOT_FOUND, message);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
