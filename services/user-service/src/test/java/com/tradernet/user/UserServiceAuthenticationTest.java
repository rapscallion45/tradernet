package com.tradernet.user;

import com.tradernet.jpa.dao.UserDao;
import com.tradernet.jpa.entities.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceAuthenticationTest {

    @AfterEach
    void clearConfiguration() {
        System.clearProperty("tradernet.auth.maxFailedLoginAttempts");
        System.clearProperty("tradernet.auth.password.argon2.memoryKiB");
        System.clearProperty("tradernet.auth.password.argon2.iterations");
    }

    @Test
    void authenticatesThroughLockedLookupAndReturnsServiceContract() {
        final UserEntity user = user("alice", "secret");
        user.setIncorrectLoginAttempts(2);
        final StubUserDao userDao = new StubUserDao(user);
        final UserService service = new UserService(userDao, configuration());

        final Optional<AuthenticatedUser> result = service.authenticateUser("alice", "secret");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUser().getUsername());
        assertEquals(0, user.getIncorrectLoginAttempts());
        assertEquals(1, userDao.lockedLookupCount);
        assertEquals(1, userDao.saveCount);
    }

    @Test
    void persistsFailedAttemptAgainstLockedUser() {
        final UserEntity user = user("alice", "secret");
        final StubUserDao userDao = new StubUserDao(user);
        final UserService service = new UserService(userDao, configuration());

        final Optional<AuthenticatedUser> result = service.authenticateUser("alice", "wrong");

        assertTrue(result.isEmpty());
        assertEquals(1, user.getIncorrectLoginAttempts());
        assertEquals(1, userDao.lockedLookupCount);
        assertEquals(1, userDao.saveCount);
    }

    @Test
    void locksAtTheConfiguredThresholdAndRecoversAfterExpiry() {
        final UserEntity user = user("alice", "secret");
        user.setIncorrectLoginAttempts(2);
        final StubUserDao userDao = new StubUserDao(user);
        final UserService service = new UserService(userDao, configuration());

        assertTrue(service.authenticateUser("alice", "wrong").isEmpty());
        assertTrue(user.getLockoutUntil().isAfter(Instant.now()));
        assertTrue(service.authenticateUser("alice", "secret").isEmpty());

        user.setLockoutUntil(Instant.now().minusSeconds(1));
        assertTrue(service.authenticateUser("alice", "secret").isPresent());
        assertEquals(0, user.getIncorrectLoginAttempts());
        assertTrue(user.getLockoutUntil() == null);
    }

    private UserEntity user(String username, String password) {
        final UserEntity user = new UserEntity(username);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        return user;
    }

    private UserSecurityConfiguration configuration() {
        System.setProperty("tradernet.auth.maxFailedLoginAttempts", "3");
        System.setProperty("tradernet.auth.password.argon2.memoryKiB", "12288");
        System.setProperty("tradernet.auth.password.argon2.iterations", "1");
        final UserSecurityConfiguration configuration = new UserSecurityConfiguration();
        configuration.load();
        return configuration;
    }

    private static final class StubUserDao implements UserDao {
        private final UserEntity user;
        private int lockedLookupCount;
        private int saveCount;

        private StubUserDao(UserEntity user) {
            this.user = user;
        }

        @Override
        public UserEntity save(UserEntity value) {
            saveCount += 1;
            return value;
        }

        @Override
        public List<UserEntity> findAll() {
            return List.of(user);
        }

        @Override
        public Optional<UserEntity> findById(long id) {
            return Optional.of(user);
        }

        @Override
        public Optional<UserEntity> findByIdForUpdate(long id) {
            return Optional.of(user);
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
            return Optional.of(user);
        }

        @Override
        public Optional<UserEntity> findByUsernameWithRoles(String username) {
            return Optional.of(user);
        }

        @Override
        public Optional<UserEntity> findByUsernameWithRolesForUpdate(String username) {
            lockedLookupCount += 1;
            return Optional.of(user);
        }

        @Override
        public void deleteAll() {
        }
    }
}
