package com.tradernet.user;

import com.tradernet.jpa.entities.AuthSessionEntity;
import com.tradernet.jpa.entities.PasswordResetSessionEntity;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns authenticated web sessions and short-lived password reset sessions.
 */
@Stateless
public class AuthSessionService {

    public static final Duration SESSION_DURATION = Duration.ofHours(8);
    public static final Duration PASSWORD_RESET_DURATION = Duration.ofMinutes(10);

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @EJB
    private UserService userService;

    public String createSession(AuthUserDto authUser) {
        final String token = UUID.randomUUID().toString();
        final AuthSessionEntity session = new AuthSessionEntity();
        session.setToken(token);
        session.setUserId(authUser.getId());
        session.setExpiresAt(Instant.now().plus(SESSION_DURATION));
        entityManager.persist(session);
        return token;
    }

    public String createPasswordResetSession(String username) {
        final String resetToken = UUID.randomUUID().toString();
        final PasswordResetSessionEntity resetSession = new PasswordResetSessionEntity();
        resetSession.setToken(resetToken);
        resetSession.setUsername(username);
        resetSession.setExpiresAt(Instant.now().plus(PASSWORD_RESET_DURATION));
        entityManager.persist(resetSession);
        return resetToken;
    }

    public Optional<AuthUserDto> getSessionUser(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }

        final AuthSessionEntity session = entityManager.find(AuthSessionEntity.class, sessionId);
        if (session == null) {
            return Optional.empty();
        }

        if (isExpired(session.getExpiresAt())) {
            entityManager.remove(session);
            return Optional.empty();
        }

        Optional<AuthUserDto> authUser = userService.findByIdWithRoles(session.getUserId())
            .map(AuthUserDto::fromUser);
        if (authUser.isEmpty()) {
            entityManager.remove(session);
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
        final AuthSessionEntity session = entityManager.find(AuthSessionEntity.class, sessionId);
        if (session != null) {
            entityManager.remove(session);
        }
    }

    public boolean isValidPasswordResetSession(String resetToken, String username) {
        if (resetToken == null || resetToken.isBlank()) {
            return false;
        }

        final PasswordResetSessionEntity resetSession = entityManager.find(PasswordResetSessionEntity.class, resetToken);
        if (resetSession == null) {
            return false;
        }

        if (isExpired(resetSession.getExpiresAt())) {
            entityManager.remove(resetSession);
            return false;
        }

        return resetSession.getUsername() != null && resetSession.getUsername().equalsIgnoreCase(username);
    }

    public void removePasswordResetSession(String resetToken) {
        if (resetToken == null || resetToken.isBlank()) {
            return;
        }
        final PasswordResetSessionEntity resetSession = entityManager.find(PasswordResetSessionEntity.class, resetToken);
        if (resetSession != null) {
            entityManager.remove(resetSession);
        }
    }

    private boolean isExpired(Instant expiresAt) {
        return expiresAt == null || expiresAt.isBefore(Instant.now());
    }
}
