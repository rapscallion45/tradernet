package com.tradernet.user;

import com.tradernet.jpa.dao.AuthSessionDao;
import com.tradernet.jpa.dao.PasswordResetSessionDao;
import com.tradernet.jpa.dao.UserDao;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Owns authenticated web sessions and short-lived password reset sessions.
 */
@Stateless
public class AuthSessionService {

    private static final int TOKEN_BYTES = 32;
    private static final Duration SESSION_TOUCH_INTERVAL = Duration.ofMinutes(1);
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final SecureRandom secureRandom;
    private final Clock clock;

    @EJB
    private AuthSessionDao authSessionDao;

    @EJB
    private PasswordResetSessionDao passwordResetSessionDao;

    @EJB
    private UserDao userDao;

    @EJB
    private UserService userService;

    @EJB
    private UserSecurityConfiguration configuration;

    @EJB
    private AuthenticationAuditService auditService;

    public AuthSessionService() {
        this(new SecureRandom(), Clock.systemUTC());
    }

    AuthSessionService(SecureRandom secureRandom, Clock clock) {
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    AuthSessionService(
        AuthSessionDao authSessionDao,
        PasswordResetSessionDao passwordResetSessionDao,
        UserDao userDao,
        UserService userService,
        UserSecurityConfiguration configuration,
        AuthenticationAuditService auditService,
        SecureRandom secureRandom,
        Clock clock
    ) {
        this(secureRandom, clock);
        this.authSessionDao = authSessionDao;
        this.passwordResetSessionDao = passwordResetSessionDao;
        this.userDao = userDao;
        this.userService = userService;
        this.configuration = configuration;
        this.auditService = auditService;
    }

    public String createSession(AuthUserDto authUser) {
        final Instant now = clock.instant();
        final String token = newToken();
        final AuthSessionEntity session = new AuthSessionEntity();
        session.setTokenHash(hashToken(token));
        session.setUserId(authUser.getId());
        session.setCreatedAt(now);
        session.setLastAccessedAt(now);
        session.setExpiresAt(now.plus(configuration.getSessionAbsoluteDuration()));
        authSessionDao.save(session);
        return token;
    }

    public String createPasswordResetSession(long userId) {
        if (userId <= 0 || userDao.findByIdForUpdate(userId).isEmpty()) {
            throw new IllegalArgumentException("Cannot create a password reset session for an unknown user");
        }

        final Instant now = clock.instant();
        final String resetToken = newToken();
        final PasswordResetSessionEntity resetSession = passwordResetSessionDao.findByUserIdForUpdate(userId)
            .orElseGet(PasswordResetSessionEntity::new);
        resetSession.setUserId(userId);
        resetSession.setTokenHash(hashToken(resetToken));
        resetSession.setExpiresAt(now.plus(configuration.getPasswordResetDuration()));
        passwordResetSessionDao.save(resetSession);
        return resetToken;
    }

    public Optional<AuthUserDto> getSessionUser(String sessionId) {
        return resolveSessionUser(sessionId, true);
    }

    /**
     * Revalidates a long-lived connection without treating the validation itself as user activity.
     */
    public Optional<AuthUserDto> validateSessionUser(String sessionId) {
        return resolveSessionUser(sessionId, false);
    }

    private Optional<AuthUserDto> resolveSessionUser(String sessionId, boolean touchSession) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }

        final String tokenHash = hashToken(sessionId);
        final Optional<AuthSessionEntity> persistedSession = authSessionDao.findByTokenHash(tokenHash);
        if (persistedSession.isEmpty()) {
            return Optional.empty();
        }
        final AuthSessionEntity session = persistedSession.get();
        final Instant now = clock.instant();

        if (isSessionExpired(session, now)) {
            authSessionDao.deleteByTokenHash(tokenHash);
            auditService.record("session_validation", "rejected", Long.toString(session.getUserId()), null, "expired");
            return Optional.empty();
        }

        final Optional<AuthUserDto> authUser = userService.getSessionEligibleUser(session.getUserId());
        if (authUser.isEmpty()) {
            authSessionDao.deleteByTokenHash(tokenHash);
            auditService.record(
                "session_validation",
                "rejected",
                Long.toString(session.getUserId()),
                null,
                "account_ineligible"
            );
            return Optional.empty();
        }

        if (touchSession && !session.getLastAccessedAt().plus(SESSION_TOUCH_INTERVAL).isAfter(now)) {
            session.setLastAccessedAt(now);
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

    public OptionalLong consumePasswordResetSession(String resetToken) {
        if (resetToken == null || resetToken.isBlank()) {
            return OptionalLong.empty();
        }

        final String tokenHash = hashToken(resetToken);
        final Optional<PasswordResetSessionEntity> candidate = passwordResetSessionDao.findByTokenHash(tokenHash);
        if (candidate.isEmpty()) {
            return OptionalLong.empty();
        }
        final long userId = candidate.get().getUserId();
        if (userDao.findByIdForUpdate(userId).isEmpty()) {
            return OptionalLong.empty();
        }

        final Optional<PasswordResetSessionEntity> persistedSession =
            passwordResetSessionDao.findByTokenHashForUpdate(tokenHash);
        if (persistedSession.isEmpty() || persistedSession.get().getUserId() != userId) {
            return OptionalLong.empty();
        }
        final PasswordResetSessionEntity resetSession = persistedSession.get();

        if (isExpired(resetSession.getExpiresAt(), clock.instant())) {
            passwordResetSessionDao.deleteByUserId(resetSession.getUserId());
            return OptionalLong.empty();
        }

        passwordResetSessionDao.deleteByUserId(resetSession.getUserId());
        return OptionalLong.of(userId);
    }

    public void removeSessionsForUser(long userId) {
        authSessionDao.deleteByUserId(userId);
    }

    public void removePasswordResetSessionForUser(long userId) {
        if (userId > 0) {
            passwordResetSessionDao.deleteByUserId(userId);
        }
    }

    @Schedule(hour = "*", minute = "*/15", second = "0", persistent = false)
    public void removeExpiredSessions() {
        final Instant now = clock.instant();
        authSessionDao.deleteExpired(now, now.minus(configuration.getSessionIdleDuration()));
        passwordResetSessionDao.deleteExpired(now);
    }

    public void removePasswordResetSession(String resetToken) {
        if (resetToken == null || resetToken.isBlank()) {
            return;
        }
        passwordResetSessionDao.findByTokenHashForUpdate(hashToken(resetToken))
            .ifPresent(session -> passwordResetSessionDao.deleteByUserId(session.getUserId()));
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
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
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

    private boolean isSessionExpired(AuthSessionEntity session, Instant now) {
        return isExpired(session.getExpiresAt(), now)
            || session.getLastAccessedAt() == null
            || !session.getLastAccessedAt().plus(configuration.getSessionIdleDuration()).isAfter(now);
    }

    private boolean isExpired(Instant expiresAt, Instant now) {
        return expiresAt == null || !expiresAt.isAfter(now);
    }
}
