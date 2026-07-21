package com.tradernet.user;

import jakarta.ejb.ApplicationException;

/**
 * Raised when an authenticated principal attempts a prohibited security administration operation.
 */
@ApplicationException(rollback = true)
public class AuthorizationDeniedException extends RuntimeException {

    public AuthorizationDeniedException(String message) {
        super(message);
    }
}
