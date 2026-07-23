package com.tradernet.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordSecurityServiceTest {

    @AfterEach
    void clearConfiguration() {
        System.clearProperty("tradernet.auth.password.argon2.memoryKiB");
        System.clearProperty("tradernet.auth.password.argon2.iterations");
        System.clearProperty("tradernet.auth.password.maximumBytes");
    }

    @Test
    void enforcesLengthUsernameBlocklistAndUtf8BytePolicies() {
        System.setProperty("tradernet.auth.password.maximumBytes", "256");
        final PasswordSecurityService service = service();

        assertThrows(InvalidPasswordException.class, () -> service.validateNewPassword("alice", "short-password"));
        assertThrows(
            InvalidPasswordException.class,
            () -> service.validateNewPassword("alice", "Secure-alice-password-2026")
        );
        assertThrows(
            InvalidPasswordException.class,
            () -> service.validateNewPassword("alice", "passwordpassword")
        );
        assertThrows(
            InvalidPasswordException.class,
            () -> service.validateNewPassword("alice", "\uD83D\uDD10".repeat(100))
        );
        assertDoesNotThrow(() -> service.validateNewPassword("alice", "Correct-Horse-Battery-Staple"));
        assertDoesNotThrow(() -> service.validateNewPassword("alice", "\uD83D\uDD10".repeat(64)));
    }

    @Test
    void createsArgon2idHashesAndNormalizesUnicodeBeforeVerification() {
        final PasswordSecurityService service = service();
        final String composed = "Caf\u00e9-Correct-Horse-2026";
        final String decomposed = "Cafe\u0301-Correct-Horse-2026";
        final String hash = service.hashPassword(composed);

        assertTrue(hash.startsWith("$argon2id$"));
        assertTrue(service.matches(decomposed, hash));
        assertTrue(service.isUsableHash(hash));
        assertFalse(service.needsRehash(hash));
    }

    @Test
    void acceptsLegacyBcryptAndMarksItForTransparentUpgrade() {
        final PasswordSecurityService service = service();
        final String hash = BCrypt.hashpw("Correct-Horse-Battery-Staple", BCrypt.gensalt(4));

        assertTrue(service.matches("Correct-Horse-Battery-Staple", hash));
        assertTrue(service.isUsableHash(hash));
        assertTrue(service.needsRehash(hash));
        assertFalse(service.matches("wrong-password", hash));
    }

    @Test
    void rejectsOversizedLoginInputWithoutParsingStoredHashes() {
        final PasswordSecurityService service = service();
        final String oversized = "x".repeat(300);

        assertFalse(service.isLoginInputSupported(oversized));
        assertFalse(service.matches(oversized, "not-a-hash"));
        assertDoesNotThrow(() -> service.performUnknownUserCheck(oversized));
    }

    private PasswordSecurityService service() {
        System.setProperty("tradernet.auth.password.argon2.memoryKiB", "12288");
        System.setProperty("tradernet.auth.password.argon2.iterations", "1");
        final UserSecurityConfiguration configuration = new UserSecurityConfiguration();
        configuration.load();
        return new PasswordSecurityService(configuration);
    }
}
