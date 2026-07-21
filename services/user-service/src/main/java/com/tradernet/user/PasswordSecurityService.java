package com.tradernet.user;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Owns password validation, hashing, verification, and transparent hash upgrades.
 */
@Singleton
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PasswordSecurityService {

    private static final String BLOCKLIST_RESOURCE = "/password-blocklist.txt";
    private static final String HASH_FORMAT_PROBE = "tradernet-password-hash-format-probe";
    private static final String UNKNOWN_USER_PASSWORD = "tradernet-unknown-user-password";
    private static final int ARGON2_SALT_LENGTH = 16;
    private static final int ARGON2_HASH_LENGTH = 32;

    @EJB
    private UserSecurityConfiguration configuration;

    private Argon2PasswordEncoder passwordEncoder;
    private Set<String> blockedPasswords;
    private String unknownUserPasswordHash;

    public PasswordSecurityService() {
    }

    PasswordSecurityService(UserSecurityConfiguration configuration) {
        this.configuration = configuration;
        initialize();
    }

    @PostConstruct
    void initialize() {
        passwordEncoder = new Argon2PasswordEncoder(
            ARGON2_SALT_LENGTH,
            ARGON2_HASH_LENGTH,
            configuration.getArgon2Parallelism(),
            configuration.getArgon2MemoryKiB(),
            configuration.getArgon2Iterations()
        );
        blockedPasswords = loadBlocklist();
        unknownUserPasswordHash = passwordEncoder.encode(normalizePassword(UNKNOWN_USER_PASSWORD));
    }

    public void validateNewPassword(String username, String password) {
        if (password == null || password.isBlank()) {
            throw new InvalidPasswordException("newPassword is required");
        }

        final String normalizedPassword = normalizePassword(password);
        final int characterCount = normalizedPassword.codePointCount(0, normalizedPassword.length());
        if (characterCount < configuration.getMinimumPasswordLength()) {
            throw new InvalidPasswordException(
                "newPassword must contain at least " + configuration.getMinimumPasswordLength() + " characters"
            );
        }
        if (characterCount > configuration.getMaximumPasswordLength()) {
            throw new InvalidPasswordException(
                "newPassword must not exceed " + configuration.getMaximumPasswordLength() + " characters"
            );
        }

        final int byteCount = normalizedPassword.getBytes(StandardCharsets.UTF_8).length;
        if (byteCount > configuration.getMaximumPasswordBytes()) {
            throw new InvalidPasswordException(
                "newPassword must not exceed " + configuration.getMaximumPasswordBytes() + " UTF-8 bytes"
            );
        }

        final String comparablePassword = normalizeForPolicy(normalizedPassword);
        if (blockedPasswords.contains(comparablePassword)) {
            throw new InvalidPasswordException("newPassword is commonly used or compromised");
        }

        final String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.length() >= 3 && comparablePassword.contains(normalizedUsername)) {
            throw new InvalidPasswordException("newPassword must not contain the username");
        }
    }

    public boolean isLoginInputSupported(String password) {
        if (password == null || password.isBlank()
            || password.length() > configuration.getMaximumPasswordLength() * 2) {
            return false;
        }
        final String normalizedPassword = normalizePassword(password);
        return normalizedPassword.codePointCount(0, normalizedPassword.length()) <= configuration.getMaximumPasswordLength()
            && normalizedPassword.getBytes(StandardCharsets.UTF_8).length <= configuration.getMaximumPasswordBytes();
    }

    public String hashPassword(String password) {
        if (!isLoginInputSupported(password)) {
            throw new InvalidPasswordException("password exceeds the supported maximum length");
        }
        return passwordEncoder.encode(normalizePassword(password));
    }

    public boolean matches(String password, String passwordHash) {
        if (!isLoginInputSupported(password)) {
            return false;
        }

        try {
            if (isArgon2Hash(passwordHash)) {
                return passwordEncoder.matches(normalizePassword(password), passwordHash);
            }
            if (isBcryptHash(passwordHash)) {
                return BCrypt.checkpw(password, passwordHash);
            }
        } catch (IllegalArgumentException ex) {
            performUnknownUserCheck(password);
            return false;
        }

        performUnknownUserCheck(password);
        return false;
    }

    public void performUnknownUserCheck(String password) {
        final String candidate = isLoginInputSupported(password)
            ? normalizePassword(password)
            : UNKNOWN_USER_PASSWORD;
        passwordEncoder.matches(candidate, unknownUserPasswordHash);
    }

    public boolean needsRehash(String passwordHash) {
        if (isBcryptHash(passwordHash)) {
            return true;
        }
        if (!isArgon2Hash(passwordHash)) {
            return false;
        }
        try {
            return passwordEncoder.upgradeEncoding(passwordHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isUsableHash(String passwordHash) {
        try {
            if (isArgon2Hash(passwordHash)) {
                passwordEncoder.matches(HASH_FORMAT_PROBE, passwordHash);
                return true;
            }
            if (isBcryptHash(passwordHash)) {
                BCrypt.checkpw(HASH_FORMAT_PROBE, passwordHash);
                return true;
            }
            return false;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    String normalizePassword(String password) {
        return Normalizer.normalize(password, Normalizer.Form.NFC);
    }

    private Set<String> loadBlocklist() {
        final Set<String> result = new HashSet<>();
        try (InputStream stream = PasswordSecurityService.class.getResourceAsStream(BLOCKLIST_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing password blocklist resource " + BLOCKLIST_RESOURCE);
            }
            loadBlocklist(stream, result);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load the built-in password blocklist", ex);
        }

        final String externalPath = configuration.getPasswordBlocklistPath();
        if (externalPath != null) {
            try (InputStream stream = Files.newInputStream(Path.of(externalPath))) {
                loadBlocklist(stream, result);
            } catch (IOException ex) {
                throw new IllegalStateException("Could not load password blocklist " + externalPath, ex);
            }
        }
        return Set.copyOf(result);
    }

    private void loadBlocklist(InputStream stream, Set<String> target) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String value = line.trim();
                if (!value.isEmpty() && !value.startsWith("#")) {
                    target.add(normalizeForPolicy(value));
                }
            }
        }
    }

    private String normalizeUsername(String username) {
        return username == null
            ? ""
            : Normalizer.normalize(username.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private String normalizeForPolicy(String password) {
        return Normalizer.normalize(normalizePassword(password), Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT);
    }

    private boolean isArgon2Hash(String passwordHash) {
        return passwordHash != null && passwordHash.startsWith("$argon2id$");
    }

    private boolean isBcryptHash(String passwordHash) {
        return passwordHash != null && (passwordHash.startsWith("$2a$")
            || passwordHash.startsWith("$2b$")
            || passwordHash.startsWith("$2y$"));
    }
}
