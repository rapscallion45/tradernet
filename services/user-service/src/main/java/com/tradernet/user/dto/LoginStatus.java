package com.tradernet.user.dto;

/**
 * Login status values returned by the authentication API.
 */
public enum LoginStatus {
    SUCCESS,
    INCORRECT_CREDENTIALS,
    USER_NOT_FOUND,
    INVALID_REQUEST,
    ACCOUNT_PASSWORD_EXPIRED,
    UNKNOWN
}
