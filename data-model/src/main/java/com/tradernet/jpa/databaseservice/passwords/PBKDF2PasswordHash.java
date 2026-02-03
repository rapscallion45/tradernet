package com.tradernet.jpa.databaseservice.passwords;

import java.util.Objects;

/**
 * Simple wrapper for PBKDF2 password hashes.
 */
public class PBKDF2PasswordHash implements PasswordHash {
    private final String hash;

    public PBKDF2PasswordHash(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public boolean matches(PlainTextCredentials credentials) {
        return Objects.equals(hash, credentials.getValue());
    }

    @Override
    public String toString() {
        return hash;
    }
}
