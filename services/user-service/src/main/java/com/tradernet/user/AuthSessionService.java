package com.tradernet.user;

import com.tradernet.jpa.dao.AuthSessionDao;
import com.tradernet.jpa.dao.PasswordResetSessionDao;
import com.tradernet.jpa.entities.AuthSessionEntity;
import com.tradernet.jpa.entities.PasswordResetSessionEntity;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Owns authenticated web sessions and short-lived password reset sessions.
 */
@Stateless
public class AuthSessionService {

    public static final Duration SESSION_DURATION = Duration.ofHours(8);
    public static final Duration PASSWORD_RESET_DURATION = Duration.ofMinutes(10);
    private static final int TOKEN_BYTES = 32;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();

    @EJB
    private AuthSessionDao authSessionDao;

    @EJB
    private PasswordResetSessionDao passwordResetSessionDao;

    @EJB
    private UserService userService;

    public String createSession(AuthUserDto authUser) {
        final String token = newToken();
        final AuthSessionEntity session = new AuthSessionEntity();
        session.setTokenHash(hashToken(token));
        session.setUserId(authUser.getId());
        session.setExpiresAt(Instant.now().plus(SESSION_DURATION));
        authSessionDao.save(session);
        return token;
    }

    public String createPasswordResetSession(String username) {
        passwordResetSessionDao.deleteByUsername(username);
        final String resetToken = newToken();
        final PasswordResetSessionEntity resetSession = new PasswordResetSessionEntity();
        resetSession.setTokenHash(hashToken(resetToken));
        resetSession.setUsername(username);
        resetSession.setExpiresAt(Instant.now().plus(PASSWORD_RESET_DURATION));
        passwordResetSessionDao.save(resetSession);
        return resetToken;
    }

    public Optional<AuthUserDto> getSessionUser(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }

        final String tokenHash = hashToken(sessionId);
        final Optional<AuthSessionEntity> persistedSession = authSessionDao.findByTokenHash(tokenHash);
        if (persistedSession.isEmpty()) {
            return Optional.empty();
        }
        final AuthSessionEntity session = persistedSession.get();

        if (isExpired(session.getExpiresAt())) {
            authSessionDao.deleteByTokenHash(tokenHash);
            return Optional.empty();
        }

        Optional<AuthUserDto> authUser = userService.findByIdWithRoles(session.getUserId())
            .filter(userService::isSessionEligible)
            .map(UserDtoMapper::toAuthUser);
        if (authUser.isEmpty()) {
            authSessionDao.deleteByTokenHash(tokenHash);
        }
        return authUser;
    }

    public boolean hasValidSession(String sessionId) {
        return getSessionUser(sessionId).isPresent();
    }

    public void removeSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        authSessionDao.deleteByTokenHash(hashToken(sessionId));
    }

    public boolean consumePasswordResetSession(String resetToken, String username) {
        if (resetToken == null || resetToken.isBlank() || username == null || username.isBlank()) {
            return false;
        }

        final String tokenHash = hashToken(resetToken);
        final Optional<PasswordResetSessionEntity> persistedSession =
            passwordResetSessionDao.findByTokenHashForUpdate(tokenHash);
        if (persistedSession.isEmpty()) {
            return false;
        }
        final PasswordResetSessionEntity resetSession = persistedSession.get();

        if (isExpired(resetSession.getExpiresAt())) {
            passwordResetSessionDao.deleteByTokenHash(tokenHash);
            return false;
        }

        if (resetSession.getUsername() == null || !resetSession.getUsername().equalsIgnoreCase(username)) {
            return false;
        }

        passwordResetSessionDao.deleteByTokenHash(tokenHash);
        return true;
    }

    public void removeSessionsForUser(long userId) {
        authSessionDao.deleteByUserId(userId);
    }

    @Schedule(hour = "*", minute = "*/15", second = "0", persistent = false)
    public void removeExpiredSessions() {
        final Instant now = Instant.now();
        authSessionDao.deleteExpired(now);
        passwordResetSessionDao.deleteExpired(now);
    }

    public void removePasswordResetSession(String resetToken) {
        if (resetToken == null || resetToken.isBlank()) {
            return;
        }
        passwordResetSessionDao.deleteByTokenHash(hashToken(resetToken));
    }

    private String newToken() {
        final byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String token) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable.", ex);
        }
    }

    private String toHex(byte[] bytes) {
        final char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i += 1) {
            final int value = bytes[i] & 0xff;
            result[i * 2] = HEX[value >>> 4];
            result[i * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(result);
    }

    private boolean isExpired(Instant expiresAt) {
        return expiresAt == null || expiresAt.isBefore(Instant.now());
    }
}
