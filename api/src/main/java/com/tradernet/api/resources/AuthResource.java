package com.tradernet.api.resources;

import com.tradernet.user.AuthenticationResult;
import com.tradernet.user.AuthenticationService;
import com.tradernet.user.AuthSessionService;
import com.tradernet.user.PasswordResetResult;
import com.tradernet.user.dto.ForgotPasswordRequestDto;
import com.tradernet.user.dto.AuthUserDto;
import com.tradernet.user.dto.LoginRequestDto;
import com.tradernet.user.dto.LoginResponseDto;
import com.tradernet.user.dto.LoginStatus;
import com.tradernet.user.dto.MessageResponseDto;
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

import java.util.Optional;

/**
 * REST API for authentication workflows.
 */
@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    public static final String SESSION_COOKIE_NAME = "tradernet_session";
    public static final String PASSWORD_RESET_COOKIE_NAME = "tradernet_password_reset";

    @EJB
    private AuthSessionService authSessionService;

    @EJB
    private AuthenticationService authenticationService;

    @POST
    @Path("/login")
    public Response login(LoginRequestDto request) {
        if (request == null) {
            return Response.ok(new LoginResponseDto(LoginStatus.INVALID_REQUEST)).build();
        }

        AuthenticationResult result = authenticationService.login(request.getUsername(), request.getPassword());
        if (result.getStatus() == LoginStatus.ACCOUNT_PASSWORD_EXPIRED) {
            return Response.ok(new LoginResponseDto(LoginStatus.ACCOUNT_PASSWORD_EXPIRED))
                .cookie(passwordResetCookie(result.getPasswordResetToken()), clearSessionCookie())
                .build();
        }

        if (result.getStatus() != LoginStatus.SUCCESS) {
            return Response.ok(new LoginResponseDto(result.getStatus())).build();
        }

        return Response.ok(new LoginResponseDto(LoginStatus.SUCCESS))
            .cookie(sessionCookie(result.getSessionToken()), clearPasswordResetCookie())
            .build();
    }

    @POST
    @Path("/logout")
    public Response logout(
        @CookieParam(SESSION_COOKIE_NAME) String sessionId,
        @CookieParam(PASSWORD_RESET_COOKIE_NAME) String passwordResetToken
    ) {
        if (sessionId != null) {
            authSessionService.removeSession(sessionId);
        }
        if (passwordResetToken != null) {
            authSessionService.removePasswordResetSession(passwordResetToken);
        }
        return Response.ok(new MessageResponseDto("Logged out"))
            .cookie(clearSessionCookie(), clearPasswordResetCookie())
            .build();
    }

    @GET
    @Path("/session")
    public Response getSession(@CookieParam(SESSION_COOKIE_NAME) String sessionId) {
        Optional<AuthUserDto> user = authSessionService.getSessionUser(sessionId);
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
        PasswordResetResult result = authenticationService.resetPassword(passwordResetToken, username, newPassword);
        if (result.getStatus() == PasswordResetResult.Status.INVALID_REQUEST) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new MessageResponseDto(result.getMessage()))
                .build();
        }

        if (result.getStatus() == PasswordResetResult.Status.INVALID_SESSION) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new MessageResponseDto(result.getMessage()))
                .cookie(clearPasswordResetCookie())
                .build();
        }

        if (result.getStatus() == PasswordResetResult.Status.USER_NOT_FOUND) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new MessageResponseDto(result.getMessage()))
                .cookie(clearPasswordResetCookie())
                .build();
        }

        return Response.ok(new MessageResponseDto(result.getMessage()))
            .cookie(clearPasswordResetCookie())
            .build();
    }

    private static NewCookie sessionCookie(String token) {
        return new NewCookie.Builder(SESSION_COOKIE_NAME)
            .value(token)
            .path("/")
            .maxAge((int) AuthSessionService.SESSION_DURATION.getSeconds())
            .httpOnly(true)
            .build();
    }

    private static NewCookie passwordResetCookie(String token) {
        return new NewCookie.Builder(PASSWORD_RESET_COOKIE_NAME)
            .value(token)
            .path("/")
            .maxAge((int) AuthSessionService.PASSWORD_RESET_DURATION.getSeconds())
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

}
