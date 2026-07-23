package com.tradernet.user.dto;

/**
 * Login status values returned by the authentication API.
 */
public enum LoginStatus {
    SUCCESS,
    INCORRECT_CREDENTIALS,
    INVALID_REQUEST,
    RATE_LIMITED,
    ACCOUNT_PASSWORD_EXPIRED,
    UNKNOWN
}
