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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.Locale;
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
    private static final String COOKIE_SECURE_PROPERTY = "tradernet.auth.cookie.secure";

    @EJB
    private AuthSessionService authSessionService;

    @EJB
    private AuthenticationService authenticationService;

    @POST
    @Path("/login")
    public Response login(
        LoginRequestDto request,
        @jakarta.ws.rs.core.Context HttpHeaders headers,
        @jakarta.ws.rs.core.Context UriInfo uriInfo
    ) {
        if (request == null) {
            return Response.ok(new LoginResponseDto(LoginStatus.INVALID_REQUEST)).build();
        }

        final boolean secureCookie = useSecureCookies(headers, uriInfo);
        AuthenticationResult result = authenticationService.login(request.getUsername(), request.getPassword());
        if (result.getStatus() == LoginStatus.ACCOUNT_PASSWORD_EXPIRED) {
            return Response.ok(new LoginResponseDto(LoginStatus.ACCOUNT_PASSWORD_EXPIRED))
                .cookie(passwordResetCookie(result.getPasswordResetToken(), secureCookie), clearSessionCookie(secureCookie))
                .build();
        }

        if (result.getStatus() != LoginStatus.SUCCESS) {
            return Response.ok(new LoginResponseDto(result.getStatus())).build();
        }

        return Response.ok(new LoginResponseDto(LoginStatus.SUCCESS))
            .cookie(sessionCookie(result.getSessionToken(), secureCookie), clearPasswordResetCookie(secureCookie))
            .build();
    }

    @POST
    @Path("/logout")
    public Response logout(
        @CookieParam(SESSION_COOKIE_NAME) String sessionId,
        @CookieParam(PASSWORD_RESET_COOKIE_NAME) String passwordResetToken,
        @jakarta.ws.rs.core.Context HttpHeaders headers,
        @jakarta.ws.rs.core.Context UriInfo uriInfo
    ) {
        if (sessionId != null) {
            authSessionService.removeSession(sessionId);
        }
        if (passwordResetToken != null) {
            authSessionService.removePasswordResetSession(passwordResetToken);
        }
        final boolean secureCookie = useSecureCookies(headers, uriInfo);
        return Response.ok(new MessageResponseDto("Logged out"))
            .cookie(clearSessionCookie(secureCookie), clearPasswordResetCookie(secureCookie))
            .build();
    }

    @GET
    @Path("/session")
    public Response getSession(@CookieParam(SESSION_COOKIE_NAME) String sessionId) {
        Optional<AuthUserDto> user = authSessionService.getSessionUser(sessionId);
        if (user.isEmpty()) {
            return ApiErrors.response(Response.Status.UNAUTHORIZED, "Not authenticated");
        }

        return Response.ok(user.get()).build();
    }

    @POST
    @Path("/forgot-password")
    public Response forgotPassword(
        @CookieParam(PASSWORD_RESET_COOKIE_NAME) String passwordResetToken,
        ForgotPasswordRequestDto request,
        @jakarta.ws.rs.core.Context HttpHeaders headers,
        @jakarta.ws.rs.core.Context UriInfo uriInfo
    ) {
        final boolean secureCookie = useSecureCookies(headers, uriInfo);
        if (request == null) {
            return ApiErrors.response(Response.Status.BAD_REQUEST, "Forgot password payload is required");
        }

        String username = request.getUsername();
        String newPassword = request.getNewPassword();
        PasswordResetResult result = authenticationService.resetPassword(passwordResetToken, username, newPassword);
        if (result.getStatus() == PasswordResetResult.Status.INVALID_REQUEST) {
            return ApiErrors.response(Response.Status.BAD_REQUEST, result.getMessage());
        }

        if (result.getStatus() == PasswordResetResult.Status.INVALID_SESSION) {
            return ApiErrors.status(Response.Status.UNAUTHORIZED, result.getMessage())
                .cookie(clearPasswordResetCookie(secureCookie))
                .build();
        }

        if (result.getStatus() == PasswordResetResult.Status.USER_NOT_FOUND) {
            return ApiErrors.status(Response.Status.NOT_FOUND, result.getMessage())
                .cookie(clearPasswordResetCookie(secureCookie))
                .build();
        }

        return Response.ok(new MessageResponseDto(result.getMessage()))
            .cookie(clearPasswordResetCookie(secureCookie))
            .build();
    }

    private static NewCookie sessionCookie(String token, boolean secure) {
        return new NewCookie.Builder(SESSION_COOKIE_NAME)
            .value(token)
            .path("/")
            .maxAge((int) AuthSessionService.SESSION_DURATION.getSeconds())
            .secure(secure)
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.LAX)
            .build();
    }

    private static NewCookie passwordResetCookie(String token, boolean secure) {
        return new NewCookie.Builder(PASSWORD_RESET_COOKIE_NAME)
            .value(token)
            .path("/")
            .maxAge((int) AuthSessionService.PASSWORD_RESET_DURATION.getSeconds())
            .secure(secure)
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.LAX)
            .build();
    }

    private static NewCookie clearSessionCookie(boolean secure) {
        return new NewCookie.Builder(SESSION_COOKIE_NAME)
            .value("")
            .path("/")
            .maxAge(0)
            .secure(secure)
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.LAX)
            .build();
    }

    private static NewCookie clearPasswordResetCookie(boolean secure) {
        return new NewCookie.Builder(PASSWORD_RESET_COOKIE_NAME)
            .value("")
            .path("/")
            .maxAge(0)
            .secure(secure)
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.LAX)
            .build();
    }

    private static boolean useSecureCookies(HttpHeaders headers, UriInfo uriInfo) {
        final String configured = System.getProperty(COOKIE_SECURE_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Boolean.parseBoolean(configured);
        }

        if (uriInfo != null && "https".equalsIgnoreCase(uriInfo.getRequestUri().getScheme())) {
            return true;
        }

        final String forwardedProto = headers == null ? null : headers.getHeaderString("X-Forwarded-Proto");
        if (forwardedProto == null || forwardedProto.isBlank()) {
            return false;
        }

        for (String value : forwardedProto.split(",")) {
            if ("https".equals(value.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

}
