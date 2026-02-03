package com.tradernet.jpa.dao.interceptors;

import com.tradernet.entities.UserEntity;

public final class PasswordMetadataInterceptor {

    private PasswordMetadataInterceptor() {
    }

    public static void populatePasswordMetadata(UserEntity user) {
        if (user == null) {
            return;
        }
        // placeholder for password metadata population
    }
}
