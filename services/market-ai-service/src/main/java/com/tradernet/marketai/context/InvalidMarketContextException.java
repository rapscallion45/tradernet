package com.tradernet.marketai.context;

import jakarta.ejb.ApplicationException;

/**
 * Raised when a manual market-context update does not contain valid normalized inputs.
 */
@ApplicationException(rollback = true)
public class InvalidMarketContextException extends IllegalArgumentException {

    public InvalidMarketContextException(String message) {
        super(message);
    }
}
