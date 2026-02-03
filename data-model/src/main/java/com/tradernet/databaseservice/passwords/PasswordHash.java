package com.tradernet.databaseservice.passwords;

public interface PasswordHash {
    boolean matches(PlainTextCredentials credentials);
}
