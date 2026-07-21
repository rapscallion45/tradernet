package com.tradernet.user;

import com.tradernet.jpa.dao.UserDao;
import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.user.dto.UserProfileDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing application users.
 * <p>
 * Provides methods for retrieving users by username or id,
 * and resolving authenticated users for the login workflow.
 */
@Stateless
public class UserService {

    private static final int DEFAULT_MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final String UNKNOWN_USER_PASSWORD_HASH = BCrypt.hashpw(
        "tradernet-unknown-user-password",
        BCrypt.gensalt()
    );

    @EJB
    private UserDao userDao;

    /**
     * Finds a user by their username.
     *
     * @param username The username to search for
     * @return Optional containing the User if found, empty otherwise
     */
    public Optional<UserEntity> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        return userDao.findByUsername(username);
    }

    /**
     * Finds a user by username and eagerly loads roles to avoid lazy-loading issues
     * when accessed outside of an active persistence context.
     *
     * @param username The username to search for
     * @return Optional containing the User if found, empty otherwise
     */
    public Optional<UserEntity> findByUsernameWithRoles(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        return userDao.findByUsernameWithRoles(username);
    }

    /**
     * Finds all users and eagerly loads roles.
     *
     * @return List of users with roles loaded
     */
    public List<UserEntity> findAllWithRoles() {
        return userDao.findAllWithRoles();
    }

    public List<UserProfileDto> getUserProfiles() {
        return findAllWithRoles().stream()
            .map(UserDtoMapper::toUserProfile)
            .collect(Collectors.toList());
    }

    /**
     * Finds a user by id and eagerly loads roles.
     *
     * @param id User id
     * @return Optional containing the User if found, empty otherwise
     */
    public Optional<UserEntity> findByIdWithRoles(long id) {
        return userDao.findByIdWithRoles(id);
    }

    public Optional<UserProfileDto> getUserProfile(long id) {
        return findByIdWithRoles(id).map(UserDtoMapper::toUserProfile);
    }

    public Optional<UserProfileDto> getUserProfileByUsername(String username) {
        return findByUsernameWithRoles(username).map(UserDtoMapper::toUserProfile);
    }

    public Optional<UserEntity> findAuthenticatedUser(String username, String password) {
        if (password == null || password.isBlank()) {
            return Optional.empty();
        }

        final Optional<UserEntity> candidate = findByUsernameWithRoles(username);
        if (candidate.isEmpty()) {
            BCrypt.checkpw(password, UNKNOWN_USER_PASSWORD_HASH);
            return Optional.empty();
        }

        final UserEntity user = candidate.get();
        if (!passwordMatches(user, password)) {
            user.setIncorrectLoginAttempts(Math.min(
                maxFailedLoginAttempts(),
                user.getIncorrectLoginAttempts() + 1
            ));
            userDao.save(user);
            return Optional.empty();
        }
        if (!isAccountAccessible(user)) {
            return Optional.empty();
        }

        user.registerSuccessfulLogin();
        userDao.save(user);
        return Optional.of(user);
    }

    /**
     * Applies the same persisted account policy to login and existing-session checks.
     */
    public boolean isAccountAccessible(UserEntity user) {
        if (user == null || user.isDeleted() || user.isDisabled() || user.isAccountExpired() || user.isLockedOut()) {
            return false;
        }
        return user.isBypassLockout() || user.getIncorrectLoginAttempts() < maxFailedLoginAttempts();
    }

    public boolean isSessionEligible(UserEntity user) {
        return isAccountAccessible(user) && !user.isChangePasswordNextLogin();
    }

    private boolean passwordMatches(UserEntity user, String password) {
        String passwordHash = user.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }

        try {
            return BCrypt.checkpw(password, passwordHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Resets a user's password (forgotten password flow).
     *
     * @param username    The username to reset
     * @param newPassword The new plain-text password
     */
    public long resetPassword(String username, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("newPassword is required");
        }

        UserEntity user = findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPasswordHash(hashedPassword);
        user.setChangePasswordNextLogin(false);
        user.resetIncorrectLoginAttempts();
        userDao.save(user);
        return user.getPk();
    }

    private int maxFailedLoginAttempts() {
        final String configured = System.getProperty(
            "tradernet.auth.maxFailedLoginAttempts",
            String.valueOf(DEFAULT_MAX_FAILED_LOGIN_ATTEMPTS)
        );
        try {
            return Math.max(1, Integer.parseInt(configured));
        } catch (NumberFormatException ex) {
            return DEFAULT_MAX_FAILED_LOGIN_ATTEMPTS;
        }
    }

}
