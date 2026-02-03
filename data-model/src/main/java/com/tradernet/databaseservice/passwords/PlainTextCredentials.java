package com.tradernet.databaseservice.passwords;

/**
 * Value object representing unencrypted credentials supplied by a user.
 */
public class PlainTextCredentials {
    private final String value;

    public PlainTextCredentials(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
