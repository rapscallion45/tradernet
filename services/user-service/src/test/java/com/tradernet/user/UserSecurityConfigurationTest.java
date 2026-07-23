package com.tradernet.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UserSecurityConfigurationTest {

    @AfterEach
    void clearConfiguration() {
        System.clearProperty("tradernet.auth.session.idleSeconds");
        System.clearProperty("tradernet.auth.session.absoluteSeconds");
        System.clearProperty("tradernet.auth.rateLimit.windowSeconds");
        System.clearProperty("tradernet.bootstrap.allowDefaultPassword");
    }

    @Test
    void rejectsMalformedNumericConfiguration() {
        System.setProperty("tradernet.auth.rateLimit.windowSeconds", "five-minutes");

        assertThrows(IllegalStateException.class, () -> newConfiguration().load());
    }

    @Test
    void rejectsIdleTimeoutLongerThanAbsoluteTimeout() {
        System.setProperty("tradernet.auth.session.idleSeconds", "600");
        System.setProperty("tradernet.auth.session.absoluteSeconds", "300");

        assertThrows(IllegalStateException.class, () -> newConfiguration().load());
    }

    @Test
    void rejectsMalformedBooleanConfiguration() {
        System.setProperty("tradernet.bootstrap.allowDefaultPassword", "yes");

        assertThrows(IllegalStateException.class, () -> newConfiguration().load());
    }

    private UserSecurityConfiguration newConfiguration() {
        return new UserSecurityConfiguration();
    }
}
