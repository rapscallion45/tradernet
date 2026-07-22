package com.tradernet.user;

import com.tradernet.jpa.dao.UserDao;
import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Owns password authentication, account eligibility, and password changes.
 */
@Stateless
public class CredentialService {

    @EJB
    private UserDao userDao;

    @EJB
    private UserSecurityPolicy configuration;

    @EJB
    private PasswordSecurityService passwordSecurityService;

    @EJB
    private AuthenticationAudit auditService;

    public CredentialService() {
    }

    CredentialService(UserSecurityPolicy configuration) {
        this.configuration = configuration;
        this.passwordSecurityService = new PasswordSecurityService(configuration);
        this.auditService = new AuthenticationAuditService();
    }

    CredentialService(UserDao userDao, UserSecurityPolicy configuration) {
        this.userDao = userDao;
        this.configuration = configuration;
        this.passwordSecurityService = new PasswordSecurityService(configuration);
        this.auditService = new AuthenticationAuditService();
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

    boolean isAccountAccessible(UserEntity user) {
        return hasUsableAccountState(user)
            && (user.isBypassLockout() || !user.isLockedOutAt(Instant.now()));
    }

    boolean isSessionEligible(UserEntity user) {
        return isAccountAccessible(user) && !user.isChangePasswordNextLogin();
    }

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
