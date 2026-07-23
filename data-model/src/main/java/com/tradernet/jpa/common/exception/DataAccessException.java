package com.tradernet.jpa.common.exception;

import jakarta.ejb.ApplicationException;

/**
 * Reports a persistence adapter failure without exposing vendor-specific exceptions.
 */
@ApplicationException(rollback = true)
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
