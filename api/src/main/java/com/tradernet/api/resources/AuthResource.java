package com.tradernet.api.resources;

import com.tradernet.user.dto.AuthUserDto;
import com.tradernet.user.dto.ForgotPasswordRequestDto;
import com.tradernet.user.dto.LoginRequestDto;
import com.tradernet.user.dto.LoginResponseDto;
import com.tradernet.user.dto.LoginStatus;
import com.tradernet.user.dto.MessageResponseDto;
import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.user.UserService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * REST API for authentication workflows.
 */
@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    public static final String SESSION_COOKIE_NAME = "tradernet_session";
    public static final String PASSWORD_RESET_COOKIE_NAME = "tradernet_password_reset";
    private static final Duration SESSION_DURATION = Duration.ofHours(8);
    private static final Duration PASSWORD_RESET_DURATION = Duration.ofMinutes(10);
    private static final Map<String, AuthUserDto> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<String, PasswordResetSession> PASSWORD_RESET_SESSIONS = new ConcurrentHashMap<>();

    @EJB
    private UserService userService;

    @POST
    @Path("/login")
    public Response login(LoginRequestDto request) {
        if (request == null) {
            return Response.ok(new LoginResponseDto(LoginStatus.INVALID_REQUEST)).build();
        }

        if (userService == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new MessageResponseDto("User service unavailable"))
                .build();
        }

        String username = request.getUsername();
        String password = request.getPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Response.ok(new LoginResponseDto(LoginStatus.INVALID_REQUEST)).build();
        }

        if (!userService.authenticate(username, password)) {
            return Response.ok(new LoginResponseDto(LoginStatus.INCORRECT_CREDENTIALS)).build();
        }

        Optional<UserEntity> user = userService.findByUsernameWithRoles(username);
        if (user.isEmpty()) {
            return Response.ok(new LoginResponseDto(LoginStatus.USER_NOT_FOUND)).build();
        }

        UserEntity authenticatedUser = user.get();
        if (isAccountBlocked(authenticatedUser)) {
            return Response.ok(new LoginResponseDto(LoginStatus.INCORRECT_CREDENTIALS)).build();
        }

        if (authenticatedUser.isChangePasswordNextLogin()) {
            String resetToken = UUID.randomUUID().toString();
            PASSWORD_RESET_SESSIONS.put(resetToken, new PasswordResetSession(
                authenticatedUser.getUsername(),
                Instant.now().plus(PASSWORD_RESET_DURATION)
            ));

            NewCookie resetCookie = new NewCookie.Builder(PASSWORD_RESET_COOKIE_NAME)
                .value(resetToken)
                .path("/")
                .maxAge((int) PASSWORD_RESET_DURATION.getSeconds())
                .httpOnly(true)
                .build();

            return Response.ok(new LoginResponseDto(LoginStatus.ACCOUNT_PASSWORD_EXPIRED))
                .cookie(resetCookie, clearSessionCookie())
                .build();
        }

        String token = UUID.randomUUID().toString();
        AuthUserDto authUser = AuthUserDto.fromUser(authenticatedUser);
        SESSIONS.put(token, authUser);

        LoginResponseDto response = new LoginResponseDto(LoginStatus.SUCCESS);

        return Response.ok(response)
            .cookie(sessionCookie(token), clearPasswordResetCookie())
            .build();
    }

    @POST
    @Path("/logout")
    public Response logout(
        @CookieParam(SESSION_COOKIE_NAME) String sessionId,
        @CookieParam(PASSWORD_RESET_COOKIE_NAME) String passwordResetToken
    ) {
        if (sessionId != null) {
            removeSession(sessionId);
        }
        if (passwordResetToken != null) {
            PASSWORD_RESET_SESSIONS.remove(passwordResetToken);
        }
        return Response.ok(new MessageResponseDto("Logged out"))
            .cookie(clearSessionCookie(), clearPasswordResetCookie())
            .build();
    }

    @GET
    @Path("/session")
    public Response getSession(@CookieParam(SESSION_COOKIE_NAME) String sessionId) {
        Optional<AuthUserDto> user = getSessionUser(sessionId);
        if (user.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new MessageResponseDto("Not authenticated"))
                .build();
        }

        return Response.ok(user.get()).build();
    }

    @POST
    @Path("/forgot-password")
    public Response forgotPassword(
        @CookieParam(PASSWORD_RESET_COOKIE_NAME) String passwordResetToken,
        ForgotPasswordRequestDto request
    ) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new MessageResponseDto("Forgot password payload is required"))
                .build();
        }

        String username = request.getUsername();
        String newPassword = request.getNewPassword();
        if (username == null || username.isBlank() || newPassword == null || newPassword.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new MessageResponseDto("username and newPassword are required"))
                .build();
        }

        if (!isValidPasswordResetSession(passwordResetToken, username)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new MessageResponseDto("Password reset session is invalid or expired"))
                .cookie(clearPasswordResetCookie())
                .build();
        }

        try {
            userService.resetPassword(username, newPassword);
        } catch (IllegalArgumentException ex) {
            PASSWORD_RESET_SESSIONS.remove(passwordResetToken);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new MessageResponseDto(ex.getMessage()))
                .cookie(clearPasswordResetCookie())
                .build();
        }

        PASSWORD_RESET_SESSIONS.remove(passwordResetToken);
        return Response.ok(new MessageResponseDto("Password reset"))
            .cookie(clearPasswordResetCookie())
            .build();
    }

    public static Optional<AuthUserDto> getSessionUser(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SESSIONS.get(sessionId));
    }

    public static boolean hasValidSession(String sessionId) {
        return getSessionUser(sessionId).isPresent();
    }

    public static void removeSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        SESSIONS.remove(sessionId);
    }

    private static NewCookie sessionCookie(String token) {
        return new NewCookie.Builder(SESSION_COOKIE_NAME)
            .value(token)
            .path("/")
            .maxAge((int) SESSION_DURATION.getSeconds())
            .httpOnly(true)
            .build();
    }

    private static NewCookie clearSessionCookie() {
        return new NewCookie.Builder(SESSION_COOKIE_NAME)
            .value("")
            .path("/")
            .maxAge(0)
            .httpOnly(true)
            .build();
    }

    private static NewCookie clearPasswordResetCookie() {
        return new NewCookie.Builder(PASSWORD_RESET_COOKIE_NAME)
            .value("")
            .path("/")
            .maxAge(0)
            .httpOnly(true)
            .build();
    }

    private static boolean isValidPasswordResetSession(String resetToken, String username) {
        if (resetToken == null || resetToken.isBlank()) {
            return false;
        }

        PasswordResetSession resetSession = PASSWORD_RESET_SESSIONS.get(resetToken);
        if (resetSession == null) {
            return false;
        }

        if (resetSession.expiresAt.isBefore(Instant.now())) {
            PASSWORD_RESET_SESSIONS.remove(resetToken);
            return false;
        }

        return resetSession.username != null && resetSession.username.equalsIgnoreCase(username);
    }

    private static boolean isAccountBlocked(UserEntity user) {
        return user.isDeleted() || user.isDisabled() || user.isAccountExpired() || user.isLockedOut();
    }

    private static class PasswordResetSession {
        private final String username;
        private final Instant expiresAt;

        private PasswordResetSession(String username, Instant expiresAt) {
            this.username = username;
            this.expiresAt = expiresAt;
        }
    }
}
