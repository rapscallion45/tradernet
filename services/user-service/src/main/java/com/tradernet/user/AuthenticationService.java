package com.tradernet.user;

import com.tradernet.user.dto.LoginStatus;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Owns login, password-reset, throttling, audit, and session workflow decisions.
 */
@Stateless
public class AuthenticationService {

    @EJB
    private UserService userService;

    @EJB
    private AuthSessionService authSessionService;

    @EJB
    private PasswordSecurityService passwordSecurityService;

    @EJB
    private AuthenticationRateLimitService rateLimitService;

    @EJB
    private AuthenticationAuditService auditService;

    public AuthenticationResult login(String username, String password, String sourceAddress) {
        final RateLimitDecision rateLimit = rateLimitService.checkLogin(sourceAddress);
        if (!rateLimit.isAllowed()) {
            auditService.record("login", "rejected", username, sourceAddress, "source_rate_limited");
            return AuthenticationResult.rateLimited(rateLimit.getRetryAfterSeconds());
        }

        if (username == null || username.isBlank() || username.length() > 50
            || password == null || password.isBlank()) {
            auditService.record("login", "rejected", username, sourceAddress, "invalid_request");
            return AuthenticationResult.status(LoginStatus.INVALID_REQUEST);
        }

        if (!passwordSecurityService.isLoginInputSupported(password)) {
            passwordSecurityService.performUnknownUserCheck(password);
            auditService.record("login", "rejected", username, sourceAddress, "incorrect_credentials");
            return AuthenticationResult.status(LoginStatus.INCORRECT_CREDENTIALS);
        }

        final Optional<AuthenticatedUser> authenticatedUser = userService.authenticateUser(username, password, sourceAddress);
        if (authenticatedUser.isEmpty()) {
            auditService.record("login", "rejected", username, sourceAddress, "incorrect_credentials");
            return AuthenticationResult.status(LoginStatus.INCORRECT_CREDENTIALS);
        }

        final AuthenticatedUser authentication = authenticatedUser.get();
        if (authentication.isPasswordChangeRequired()) {
            final String resetToken = authSessionService.createPasswordResetSession(authentication.getUser().getId());
            auditService.record("login", "challenge", username, sourceAddress, "password_change_required");
            return AuthenticationResult.passwordExpired(resetToken);
        }

        authSessionService.removePasswordResetSessionForUser(authentication.getUser().getId());
        final String sessionToken = authSessionService.createSession(authentication.getUser());
        auditService.record("login", "success", username, sourceAddress, "authenticated");
        return AuthenticationResult.success(sessionToken);
    }

    public PasswordResetResult resetPassword(String resetToken, String newPassword, String sourceAddress) {
        final RateLimitDecision rateLimit = rateLimitService.checkPasswordReset(sourceAddress);
        if (!rateLimit.isAllowed()) {
            auditService.record("password_reset", "rejected", null, sourceAddress, "source_rate_limited");
            return PasswordResetResult.rateLimited(rateLimit.getRetryAfterSeconds());
        }

        if (newPassword == null || newPassword.isBlank()) {
            auditService.record("password_reset", "rejected", null, sourceAddress, "invalid_request");
            return PasswordResetResult.invalidRequest();
        }

        final OptionalLong resetUserId = authSessionService.consumePasswordResetSession(resetToken);
        if (resetUserId.isEmpty()) {
            auditService.record("password_reset", "rejected", null, sourceAddress, "invalid_session");
            return PasswordResetResult.invalidSession();
        }

        final long userId = resetUserId.getAsLong();
        final OptionalLong updatedUserId = userService.resetPassword(userId, newPassword);
        if (updatedUserId.isEmpty()) {
            auditService.record("password_reset", "rejected", Long.toString(userId), sourceAddress, "account_ineligible");
            return PasswordResetResult.invalidSession();
        }
        authSessionService.removeSessionsForUser(userId);
        auditService.record("password_reset", "success", Long.toString(userId), sourceAddress, "password_changed");
        return PasswordResetResult.success(userId);
    }

    public void logout(String sessionId, String passwordResetToken, String sourceAddress) {
        authSessionService.removeSession(sessionId);
        authSessionService.removePasswordResetSession(passwordResetToken);
        auditService.record("logout", "success", null, sourceAddress, "client_requested");
    }
}
