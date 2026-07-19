package com.tradernet.user;

import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.user.dto.LoginStatus;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.Optional;

/**
 * Owns login workflow decisions and session token creation.
 */
@Stateless
public class AuthenticationService {

    @EJB
    private UserService userService;

    @EJB
    private AuthSessionService authSessionService;

    public AuthenticationResult login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return AuthenticationResult.status(LoginStatus.INVALID_REQUEST);
        }

        Optional<UserEntity> authenticatedUser = userService.findAuthenticatedUser(username, password);
        if (authenticatedUser.isEmpty()) {
            return AuthenticationResult.status(LoginStatus.INCORRECT_CREDENTIALS);
        }

        UserEntity user = authenticatedUser.get();
        if (isAccountBlocked(user)) {
            return AuthenticationResult.status(LoginStatus.INCORRECT_CREDENTIALS);
        }

        if (user.isChangePasswordNextLogin()) {
            return AuthenticationResult.passwordExpired(authSessionService.createPasswordResetSession(user.getUsername()));
        }

        return AuthenticationResult.success(authSessionService.createSession(UserDtoMapper.toAuthUser(user)));
    }

    public PasswordResetResult resetPassword(String resetToken, String username, String newPassword) {
        if (username == null || username.isBlank() || newPassword == null || newPassword.isBlank()) {
            return PasswordResetResult.invalidRequest();
        }

        if (!authSessionService.isValidPasswordResetSession(resetToken, username)) {
            return PasswordResetResult.invalidSession();
        }

        try {
            userService.resetPassword(username, newPassword);
        } catch (IllegalArgumentException ex) {
            authSessionService.removePasswordResetSession(resetToken);
            return PasswordResetResult.userNotFound(ex.getMessage());
        }

        authSessionService.removePasswordResetSession(resetToken);
        return PasswordResetResult.success();
    }

    private boolean isAccountBlocked(UserEntity user) {
        return user.isDeleted() || user.isDisabled() || user.isAccountExpired() || user.isLockedOut();
    }
}
