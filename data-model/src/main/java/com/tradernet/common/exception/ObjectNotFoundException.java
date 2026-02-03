package com.tradernet.common.exception;

/**
 * Runtime exception thrown when an expected object cannot be located.
 */
public class ObjectNotFoundException extends RuntimeException {
    public ObjectNotFoundException(String message) {
        super(message);
    }
}
