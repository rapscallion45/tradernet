package com.tradernet.user;

import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.jpa.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceAccountPolicyTest {

    private static final String MAX_ATTEMPTS_PROPERTY = "tradernet.auth.maxFailedLoginAttempts";

    private final UserService userService = new UserService();

    @AfterEach
    void clearConfiguration() {
        System.clearProperty(MAX_ATTEMPTS_PROPERTY);
    }

    @Test
    void acceptsAnActiveAccountBelowTheFailedAttemptLimit() {
        UserEntity user = activeUser();
        user.setIncorrectLoginAttempts(4);

        assertTrue(userService.isAccountAccessible(user));
        assertTrue(userService.isSessionEligible(user));
    }

    @Test
    void rejectsPersistedAndTransientAccountBlocks() {
        UserEntity disabled = activeUser();
        disabled.setStatus(UserStatus.DISABLED);
        assertFalse(userService.isAccountAccessible(disabled));

        UserEntity expired = activeUser();
        expired.setAccountExpiry(new Date(System.currentTimeMillis() - 1_000L));
        assertFalse(userService.isAccountAccessible(expired));

        UserEntity locked = activeUser();
        locked.setLockedOut(true);
        assertFalse(userService.isAccountAccessible(locked));
    }

    @Test
    void appliesTheConfiguredFailedAttemptLimitUnlessBypassed() {
        System.setProperty(MAX_ATTEMPTS_PROPERTY, "3");
        UserEntity user = activeUser();
        user.setIncorrectLoginAttempts(3);

        assertFalse(userService.isAccountAccessible(user));

        user.setBypassLockout(true);
        assertTrue(userService.isAccountAccessible(user));
    }

    @Test
    void invalidatesSessionsThatRequireAPasswordChange() {
        UserEntity user = activeUser();
        user.setChangePasswordNextLogin(true);

        assertTrue(userService.isAccountAccessible(user));
        assertFalse(userService.isSessionEligible(user));
    }

    private UserEntity activeUser() {
        return new UserEntity("test-user");
    }
}
