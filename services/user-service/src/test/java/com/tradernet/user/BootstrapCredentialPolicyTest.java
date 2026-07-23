package com.tradernet.user;

import com.tradernet.jpa.entities.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BootstrapCredentialPolicyTest {

    @AfterEach
    void clearConfiguration() {
        System.clearProperty("tradernet.bootstrap.allowDefaultPassword");
        System.clearProperty("tradernet.auth.password.argon2.memoryKiB");
        System.clearProperty("tradernet.auth.password.argon2.iterations");
    }

    @Test
    void rejectsPersistedDevelopmentPasswordWhenFallbackIsDisabled() {
        final Fixture fixture = fixture(false);

        assertThrows(
            IllegalStateException.class,
            () -> fixture.policy.verifyPersistedCredential(fixture.user)
        );
    }

    @Test
    void permitsDevelopmentPasswordOnlyAfterExplicitLocalOptIn() {
        final Fixture fixture = fixture(true);

        assertDoesNotThrow(() -> fixture.policy.verifyPersistedCredential(fixture.user));
    }

    private Fixture fixture(boolean allowDefaultPassword) {
        System.setProperty("tradernet.bootstrap.allowDefaultPassword", Boolean.toString(allowDefaultPassword));
        System.setProperty("tradernet.auth.password.argon2.memoryKiB", "12288");
        System.setProperty("tradernet.auth.password.argon2.iterations", "1");
        final UserSecurityConfiguration configuration = new UserSecurityConfiguration();
        configuration.load();
        final PasswordSecurityService passwordSecurityService = new PasswordSecurityService(configuration);
        final UserEntity user = new UserEntity("superuser");
        user.setPasswordHash(passwordSecurityService.hashPassword(configuration.getInsecureBootstrapPassword()));
        return new Fixture(
            new BootstrapCredentialPolicy(configuration, passwordSecurityService),
            user
        );
    }

    private static final class Fixture {
        private final BootstrapCredentialPolicy policy;
        private final UserEntity user;

        private Fixture(BootstrapCredentialPolicy policy, UserEntity user) {
            this.policy = policy;
            this.user = user;
        }
    }
}
