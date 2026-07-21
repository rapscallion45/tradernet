package com.tradernet.user;

import com.tradernet.jpa.dao.UserDao;
import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.user.dto.AuthUserDto;
import com.tradernet.user.dto.UserProfileDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * Service for managing application users.
 * <p>
 * Provides methods for retrieving users by username or id,
 * and resolving authenticated users for the login workflow.
 */
@Stateless
public class UserService {

    @EJB
    private UserDao userDao;

    @EJB
    private UserSecurityConfiguration configuration;

    @EJB
    private PasswordSecurityService passwordSecurityService;

    @EJB
    private AuthenticationAuditService auditService;

    public UserService() {
    }

    UserService(UserSecurityConfiguration configuration) {
        this.configuration = configuration;
        this.passwordSecurityService = new PasswordSecurityService(configuration);
        this.auditService = new AuthenticationAuditService();
    }

    UserService(UserDao userDao, UserSecurityConfiguration configuration) {
        this.userDao = userDao;
        this.configuration = configuration;
        this.passwordSecurityService = new PasswordSecurityService(configuration);
        this.auditService = new AuthenticationAuditService();
    }

    public List<UserProfileDto> getUserProfiles() {
        return userDao.findAllWithRoles().stream()
            .map(UserDtoMapper::toUserProfile)
            .collect(Collectors.toList());
    }

    public Optional<UserProfileDto> getUserProfile(long id) {
        return userDao.findByIdWithRoles(id).map(UserDtoMapper::toUserProfile);
    }

    public Optional<UserProfileDto> getUserProfileByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userDao.findByUsernameWithRoles(username).map(UserDtoMapper::toUserProfile);
    }

    public Optional<AuthenticatedUser> authenticateUser(String username, String password) {
        return authenticateUser(username, password, null);
    }

    public Optional<AuthenticatedUser> authenticateUser(String username, String password, String sourceAddress) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        final Optional<UserEntity> candidate = userDao.findByUsernameWithRolesForUpdate(username);
        if (candidate.isEmpty()) {
            passwordSecurityService.performUnknownUserCheck(password);
            return Optional.empty();
        }

        final UserEntity user = candidate.get();
        final Instant now = Instant.now();
        final boolean expiredLockoutCleared = clearExpiredLockout(user, now);
        final boolean passwordMatches = passwordSecurityService.matches(password, user.getPasswordHash());

        if (!user.isBypassLockout() && user.isLockedOutAt(now)) {
            auditService.record("account_lockout", "rejected", user.getUsername(), sourceAddress, "active_lockout");
            return Optional.empty();
        }
        if (!hasUsableAccountState(user) || user.isExternalIdentity()) {
            if (expiredLockoutCleared) {
                userDao.save(user);
            }
            return Optional.empty();
        }
        if (!passwordMatches) {
            final boolean newlyLocked = registerFailedLogin(user, now);
            userDao.save(user);
            if (newlyLocked) {
                auditService.record("account_lockout", "applied", user.getUsername(), sourceAddress, "failure_threshold");
            }
            return Optional.empty();
        }

        user.registerSuccessfulLogin();
        if (!user.isChangePasswordNextLogin() && passwordSecurityService.needsRehash(user.getPasswordHash())) {
            user.setPasswordHash(passwordSecurityService.hashPassword(password));
        }
        userDao.save(user);
        return Optional.of(new AuthenticatedUser(
            UserDtoMapper.toAuthUser(user),
            user.isChangePasswordNextLogin()
        ));
    }

    public Optional<AuthUserDto> getSessionEligibleUser(long userId) {
        return userDao.findByIdWithRoles(userId)
            .filter(this::isSessionEligible)
            .map(UserDtoMapper::toAuthUser);
    }

    /**
     * Applies the same persisted account policy to login and existing-session checks.
     */
    boolean isAccountAccessible(UserEntity user) {
        return hasUsableAccountState(user)
            && (user.isBypassLockout() || !user.isLockedOutAt(Instant.now()));
    }

    boolean isSessionEligible(UserEntity user) {
        return isAccountAccessible(user) && !user.isChangePasswordNextLogin();
    }

    /**
     * Resets a user's password (forgotten password flow).
     *
     * @param username    The username to reset
     * @param newPassword The new plain-text password
     */
    public OptionalLong resetPassword(long userId, String newPassword) {
        if (userId <= 0) {
            return OptionalLong.empty();
        }

        final Optional<UserEntity> foundUser = userDao.findByIdForUpdate(userId);
        if (foundUser.isEmpty()) {
            return OptionalLong.empty();
        }

        final UserEntity user = foundUser.get();
        if (!hasUsableAccountState(user) || user.isExternalIdentity()) {
            return OptionalLong.empty();
        }
        passwordSecurityService.validateNewPassword(user.getUsername(), newPassword);
        user.setPasswordHash(passwordSecurityService.hashPassword(newPassword));
        user.setChangePasswordNextLogin(false);
        user.clearLockout();
        userDao.save(user);
        return OptionalLong.of(user.getPk());
    }

    private boolean hasUsableAccountState(UserEntity user) {
        return user != null && !user.isSystem() && !user.isDeleted() && !user.isDisabled() && !user.isAccountExpired();
    }

    private boolean clearExpiredLockout(UserEntity user, Instant now) {
        final Instant lockoutUntil = user.getLockoutUntil();
        if (lockoutUntil == null || lockoutUntil.isAfter(now)) {
            return false;
        }
        user.clearLockout();
        return true;
    }

    private boolean registerFailedLogin(UserEntity user, Instant now) {
        final int failedAttempts = Math.min(
            configuration.getMaxFailedLoginAttempts(),
            user.getIncorrectLoginAttempts() + 1
        );
        user.setIncorrectLoginAttempts(failedAttempts);
        if (!user.isBypassLockout() && failedAttempts >= configuration.getMaxFailedLoginAttempts()) {
            user.setLockoutUntil(now.plus(configuration.getLockoutDuration()));
            return true;
        }
        return false;
    }

}
