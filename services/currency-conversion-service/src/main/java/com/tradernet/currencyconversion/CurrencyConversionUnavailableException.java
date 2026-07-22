package com.tradernet.currencyconversion;

/**
 * Raised when no provider-sourced FX rate is available for a requested conversion.
 */
public class CurrencyConversionUnavailableException extends RuntimeException {

    public CurrencyConversionUnavailableException(String message) {
        super(message);
    }
}
