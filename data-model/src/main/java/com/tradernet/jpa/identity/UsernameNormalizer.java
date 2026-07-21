package com.tradernet.jpa.identity;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Defines the canonical representation used for persisted username identity.
 */
public final class UsernameNormalizer {

    private UsernameNormalizer() {
    }

    public static String normalize(String username) {
        if (username == null) {
            return null;
        }
        return Normalizer.normalize(username.trim(), Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT);
    }
}
