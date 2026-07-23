package com.tradernet.user;

import jakarta.ejb.ApplicationException;

/**
 * Raised when a proposed password does not meet the configured password policy.
 */
@ApplicationException(rollback = true)
public class InvalidPasswordException extends IllegalArgumentException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}
