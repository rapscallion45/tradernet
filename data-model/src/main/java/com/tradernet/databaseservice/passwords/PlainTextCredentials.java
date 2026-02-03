package com.tradernet.databaseservice.passwords;

public class PlainTextCredentials {
    private final String value;

    public PlainTextCredentials(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
