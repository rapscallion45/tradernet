package com.tradernet.api.resources;

/**
 * Raised when a protected resource is reached without an authenticated user.
 */
public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException() {
        super("Not authenticated");
    }
}
