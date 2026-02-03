package com.tradernet.jpa.databaseservice.passwords;

/**
 * Abstraction for stored password hashes that can validate plaintext credentials.
 */
public interface PasswordHash {
    boolean matches(PlainTextCredentials credentials);
}
