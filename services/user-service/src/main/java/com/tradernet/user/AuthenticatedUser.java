package com.tradernet.user;

import com.tradernet.user.dto.AuthUserDto;

/**
 * Persistence-independent result of a successful credential check.
 */
public final class AuthenticatedUser {

    private final AuthUserDto user;
    private final boolean passwordChangeRequired;

    public AuthenticatedUser(AuthUserDto user, boolean passwordChangeRequired) {
        this.user = user;
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public AuthUserDto getUser() {
        return user;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }
}
