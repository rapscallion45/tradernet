package com.tradernet.user;

import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.jpa.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceAccountPolicyTest {

    private static final String MAX_ATTEMPTS_PROPERTY = "tradernet.auth.maxFailedLoginAttempts";
    private static final String ARGON2_MEMORY_PROPERTY = "tradernet.auth.password.argon2.memoryKiB";
    private static final String ARGON2_ITERATIONS_PROPERTY = "tradernet.auth.password.argon2.iterations";

    private final UserSecurityConfiguration configuration = new UserSecurityConfiguration();
    private final CredentialService credentialService;

    UserServiceAccountPolicyTest() {
        System.setProperty(ARGON2_MEMORY_PROPERTY, "12288");
        System.setProperty(ARGON2_ITERATIONS_PROPERTY, "1");
        configuration.load();
        credentialService = new CredentialService(configuration);
    }

    @AfterEach
    void clearConfiguration() {
        System.clearProperty(MAX_ATTEMPTS_PROPERTY);
        System.clearProperty(ARGON2_MEMORY_PROPERTY);
        System.clearProperty(ARGON2_ITERATIONS_PROPERTY);
        configuration.load();
    }

    @Test
    void acceptsAnActiveAccountBelowTheFailedAttemptLimit() {
        UserEntity user = activeUser();
        user.setIncorrectLoginAttempts(4);

        assertTrue(credentialService.isAccountAccessible(user));
        assertTrue(credentialService.isSessionEligible(user));
    }

    @Test
    void rejectsPersistedAccountBlocks() {
        UserEntity disabled = activeUser();
        disabled.setStatus(UserStatus.DISABLED);
        assertFalse(credentialService.isAccountAccessible(disabled));

        UserEntity expired = activeUser();
        expired.setAccountExpiry(new Date(System.currentTimeMillis() - 1_000L));
        assertFalse(credentialService.isAccountAccessible(expired));

        UserEntity locked = activeUser();
        locked.setLockoutUntil(Instant.now().plusSeconds(60));
        assertFalse(credentialService.isAccountAccessible(locked));
    }

    @Test
    void appliesPersistedTemporaryLockoutUnlessBypassed() {
        UserEntity user = activeUser();
        user.setLockoutUntil(Instant.now().plusSeconds(60));

        assertFalse(credentialService.isAccountAccessible(user));

        user.setBypassLockout(true);
        assertTrue(credentialService.isAccountAccessible(user));
    }

    @Test
    void acceptsAnAccountAfterItsTemporaryLockoutExpires() {
        UserEntity user = activeUser();
        user.setIncorrectLoginAttempts(5);
        user.setLockoutUntil(Instant.now().minusSeconds(1));

        assertTrue(credentialService.isAccountAccessible(user));
    }

    @Test
    void invalidatesSessionsThatRequireAPasswordChange() {
        UserEntity user = activeUser();
        user.setChangePasswordNextLogin(true);

        assertTrue(credentialService.isAccountAccessible(user));
        assertFalse(credentialService.isSessionEligible(user));
    }

    private UserEntity activeUser() {
        return new UserEntity("test-user");
    }
}
