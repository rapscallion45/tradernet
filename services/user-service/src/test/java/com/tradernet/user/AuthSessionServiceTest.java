package com.tradernet.user;

import com.tradernet.jpa.dao.AuthSessionDao;
import com.tradernet.jpa.dao.PasswordResetSessionDao;
import com.tradernet.jpa.dao.UserDao;
import com.tradernet.jpa.entities.AuthSessionEntity;
import com.tradernet.jpa.entities.PasswordResetSessionEntity;
import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.user.dto.AuthUserDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthSessionServiceTest {

    @AfterEach
    void clearConfiguration() {
        System.clearProperty("tradernet.auth.session.absoluteSeconds");
        System.clearProperty("tradernet.auth.session.idleSeconds");
        System.clearProperty("tradernet.auth.passwordReset.durationSeconds");
    }

    @Test
    void expiresSessionsAtTheIdleBoundary() {
        final Fixture fixture = fixture();
        final String token = fixture.service.createSession(fixture.authUser);

        fixture.clock.advance(Duration.ofSeconds(120));

        assertTrue(fixture.service.getSessionUser(token).isEmpty());
        assertTrue(fixture.authSessions.session == null);
    }

    @Test
    void expiresSessionsAtTheAbsoluteBoundaryDespiteActivity() {
        final Fixture fixture = fixture();
        final String token = fixture.service.createSession(fixture.authUser);

        for (int index = 0; index < 4; index += 1) {
            fixture.clock.advance(Duration.ofSeconds(61));
            assertTrue(fixture.service.getSessionUser(token).isPresent());
        }
        fixture.clock.advance(Duration.ofSeconds(56));

        assertTrue(fixture.service.getSessionUser(token).isEmpty());
    }

    @Test
    void replacesAnyExistingPasswordResetTokenForTheUser() {
        final Fixture fixture = fixture();
        final String first = fixture.service.createPasswordResetSession(fixture.authUser.getId());
        final String second = fixture.service.createPasswordResetSession(fixture.authUser.getId());

        assertFalse(first.equals(second));
        assertTrue(fixture.service.consumePasswordResetSession(first).isEmpty());
        assertEquals(fixture.authUser.getId(), fixture.service.consumePasswordResetSession(second).orElseThrow());
        assertTrue(fixture.passwordResetSessions.session == null);
    }

    private Fixture fixture() {
        System.setProperty("tradernet.auth.session.absoluteSeconds", "300");
        System.setProperty("tradernet.auth.session.idleSeconds", "120");
        System.setProperty("tradernet.auth.passwordReset.durationSeconds", "120");
        final UserSecurityConfiguration configuration = new UserSecurityConfiguration();
        configuration.load();

        final AuthUserDto authUser = new AuthUserDto(42, "alice", Set.of("Standard Rights"));
        final InMemoryAuthSessionDao authSessions = new InMemoryAuthSessionDao();
        final InMemoryPasswordResetSessionDao resetSessions = new InMemoryPasswordResetSessionDao();
        final MutableClock clock = new MutableClock(Instant.parse("2026-07-21T10:00:00Z"));
        final AuthSessionService service = new AuthSessionService(
            authSessions,
            resetSessions,
            new ExistingUserDao(authUser.getId()),
            new EligibleCredentialService(authUser),
            configuration,
            new SilentAuditService(),
            new SecureRandom(),
            clock
        );
        return new Fixture(service, authSessions, resetSessions, clock, authUser);
    }

    private static final class Fixture {
        private final AuthSessionService service;
        private final InMemoryAuthSessionDao authSessions;
        private final InMemoryPasswordResetSessionDao passwordResetSessions;
        private final MutableClock clock;
        private final AuthUserDto authUser;

        private Fixture(
            AuthSessionService service,
            InMemoryAuthSessionDao authSessions,
            InMemoryPasswordResetSessionDao passwordResetSessions,
            MutableClock clock,
            AuthUserDto authUser
        ) {
            this.service = service;
            this.authSessions = authSessions;
            this.passwordResetSessions = passwordResetSessions;
            this.clock = clock;
            this.authUser = authUser;
        }
    }

    private static final class InMemoryAuthSessionDao implements AuthSessionDao {
        private AuthSessionEntity session;

        @Override
        public void save(AuthSessionEntity session) {
            this.session = session;
        }

        @Override
        public Optional<AuthSessionEntity> findByTokenHash(String tokenHash) {
            return session != null && tokenHash.equals(session.getTokenHash())
                ? Optional.of(session)
                : Optional.empty();
        }

        @Override
        public void deleteByTokenHash(String tokenHash) {
            if (session != null && tokenHash.equals(session.getTokenHash())) {
                session = null;
            }
        }

        @Override
        public void deleteByUserId(long userId) {
            if (session != null && session.getUserId() == userId) {
                session = null;
            }
        }

        @Override
        public int deleteExpired(Instant now, Instant idleCutoff) {
            return 0;
        }
    }

    private static final class InMemoryPasswordResetSessionDao implements PasswordResetSessionDao {
        private PasswordResetSessionEntity session;

        @Override
        public void save(PasswordResetSessionEntity session) {
            this.session = session;
        }

        @Override
        public Optional<PasswordResetSessionEntity> findByUserIdForUpdate(long userId) {
            return session != null && session.getUserId() == userId ? Optional.of(session) : Optional.empty();
        }

        @Override
        public Optional<PasswordResetSessionEntity> findByTokenHashForUpdate(String tokenHash) {
            return findByTokenHash(tokenHash);
        }

        @Override
        public Optional<PasswordResetSessionEntity> findByTokenHash(String tokenHash) {
            return session != null && tokenHash.equals(session.getTokenHash())
                ? Optional.of(session)
                : Optional.empty();
        }

        @Override
        public void deleteByUserId(long userId) {
            if (session != null && session.getUserId() == userId) {
                session = null;
            }
        }

        @Override
        public int deleteExpired(Instant now) {
            return 0;
        }
    }

    private static final class EligibleCredentialService extends CredentialService {
        private final AuthUserDto user;

        private EligibleCredentialService(AuthUserDto user) {
            this.user = user;
        }

        @Override
        public Optional<AuthUserDto> getSessionEligibleUser(long userId) {
            return user.getId() == userId ? Optional.of(user) : Optional.empty();
        }
    }

    private static final class SilentAuditService extends AuthenticationAuditService {
        @Override
        public void record(String event, String outcome, String subject, String sourceAddress, String reason) {
        }
    }

    private static final class ExistingUserDao implements UserDao {
        private final UserEntity user;

        private ExistingUserDao(long userId) {
            user = new UserEntity("alice");
            user.setPk(userId);
        }

        @Override
        public UserEntity save(UserEntity user) {
            return user;
        }

        @Override
        public List<UserEntity> findAll() {
            return List.of(user);
        }

        @Override
        public Optional<UserEntity> findById(long id) {
            return id == user.getPk() ? Optional.of(user) : Optional.empty();
        }

        @Override
        public Optional<UserEntity> findByIdForUpdate(long id) {
            return findById(id);
        }

        @Override
        public Optional<UserEntity> findByUsername(String username) {
            return Optional.of(user);
        }

        @Override
        public List<UserEntity> findByUsernames(Set<String> usernames) {
            return List.of(user);
        }

        @Override
        public List<UserEntity> findAllWithRoles() {
            return List.of(user);
        }

        @Override
        public Optional<UserEntity> findByIdWithRoles(long id) {
            return findById(id);
        }

        @Override
        public Optional<UserEntity> findByUsernameWithRoles(String username) {
            return Optional.of(user);
        }

        @Override
        public Optional<UserEntity> findByUsernameWithRolesForUpdate(String username) {
            return Optional.of(user);
        }

        @Override
        public void deleteAll() {
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
