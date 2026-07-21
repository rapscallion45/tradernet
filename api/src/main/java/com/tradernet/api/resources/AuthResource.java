package com.tradernet.api.resources;

import com.tradernet.api.ApiConfiguration;
import com.tradernet.user.AuthenticationResult;
import com.tradernet.user.AuthenticationService;
import com.tradernet.user.AuthSessionService;
import com.tradernet.user.PasswordResetResult;
import com.tradernet.user.UserSecurityConfiguration;
import com.tradernet.user.dto.AuthUserDto;
import com.tradernet.user.dto.ForgotPasswordRequestDto;
import com.tradernet.user.dto.LoginRequestDto;
import com.tradernet.user.dto.LoginResponseDto;
import com.tradernet.user.dto.LoginStatus;
import com.tradernet.user.dto.MessageResponseDto;
import jakarta.ejb.EJB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
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

    @EJB
    private ApiConfiguration configuration;

    @EJB
    private UserSecurityConfiguration userSecurityConfiguration;

    @EJB
    private MarketWebSocketSessionRegistry marketWebSocketSessionRegistry;

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequestDto request, @Context HttpServletRequest servletRequest) {
        if (request == null) {
            return noStore(Response.ok(new LoginResponseDto(LoginStatus.INVALID_REQUEST))).build();
        }

        final boolean secureCookie = configuration.useSecureCookies();
        final AuthenticationResult result = authenticationService.login(
            request.getUsername(),
            request.getPassword(),
            sourceAddress(servletRequest)
        );
        if (result.getStatus() == LoginStatus.RATE_LIMITED) {
            return noStore(ApiErrors.status(Response.Status.TOO_MANY_REQUESTS, "Too many login attempts"))
                .header("Retry-After", result.getRetryAfterSeconds())
                .build();
        }
        if (result.getStatus() == LoginStatus.ACCOUNT_PASSWORD_EXPIRED) {
            return noStore(Response.ok(new LoginResponseDto(LoginStatus.ACCOUNT_PASSWORD_EXPIRED)))
                .cookie(passwordResetCookie(result.getPasswordResetToken(), secureCookie), clearSessionCookie(secureCookie))
                .build();
        }
        if (result.getStatus() != LoginStatus.SUCCESS) {
            return noStore(Response.ok(new LoginResponseDto(result.getStatus()))).build();
        }

        return noStore(Response.ok(new LoginResponseDto(LoginStatus.SUCCESS)))
            .cookie(sessionCookie(result.getSessionToken(), secureCookie), clearPasswordResetCookie(secureCookie))
            .build();
    }

    @POST
    @Path("/logout")
    public Response logout(
        @CookieParam(SESSION_COOKIE_NAME) String sessionId,
        @CookieParam(PASSWORD_RESET_COOKIE_NAME) String passwordResetToken,
        @Context HttpServletRequest servletRequest
    ) {
        authenticationService.logout(sessionId, passwordResetToken, sourceAddress(servletRequest));
        marketWebSocketSessionRegistry.closeBySessionToken(sessionId);
        final boolean secureCookie = configuration.useSecureCookies();
        return noStore(Response.ok(new MessageResponseDto("Logged out")))
            .cookie(clearSessionCookie(secureCookie), clearPasswordResetCookie(secureCookie))
            .build();
    }

    @GET
    @Path("/session")
    public Response getSession(@CookieParam(SESSION_COOKIE_NAME) String sessionId) {
        final Optional<AuthUserDto> user = authSessionService.getSessionUser(sessionId);
        if (user.isEmpty()) {
            return noStore(ApiErrors.status(Response.Status.UNAUTHORIZED, "Not authenticated")).build();
        }
        return noStore(Response.ok(user.get())).build();
    }

    @POST
    @Path("/forgot-password")
    public Response forgotPassword(
        @CookieParam(PASSWORD_RESET_COOKIE_NAME) String passwordResetToken,
        @Valid ForgotPasswordRequestDto request,
        @Context HttpServletRequest servletRequest
    ) {
        final boolean secureCookie = configuration.useSecureCookies();
        if (request == null) {
            return noStore(ApiErrors.status(Response.Status.BAD_REQUEST, "Password reset payload is required")).build();
        }

        final PasswordResetResult result = authenticationService.resetPassword(
            passwordResetToken,
            request.getNewPassword(),
            sourceAddress(servletRequest)
        );
        if (result.getStatus() == PasswordResetResult.Status.RATE_LIMITED) {
            return noStore(ApiErrors.status(Response.Status.TOO_MANY_REQUESTS, result.getMessage()))
                .header("Retry-After", result.getRetryAfterSeconds())
                .build();
        }
        if (result.getStatus() == PasswordResetResult.Status.INVALID_REQUEST) {
            return noStore(ApiErrors.status(Response.Status.BAD_REQUEST, result.getMessage())).build();
        }
        if (result.getStatus() == PasswordResetResult.Status.INVALID_SESSION) {
            return noStore(ApiErrors.status(Response.Status.UNAUTHORIZED, "Password reset session is invalid or expired"))
                .cookie(clearPasswordResetCookie(secureCookie))
                .build();
        }

        marketWebSocketSessionRegistry.closeByUserId(result.getUserId());
        return noStore(Response.ok(new MessageResponseDto(result.getMessage())))
            .cookie(clearPasswordResetCookie(secureCookie))
            .build();
    }

    static NewCookie sessionCookie(String token, boolean secure) {
        return new NewCookie.Builder(SESSION_COOKIE_NAME)
            .value(token)
            .path("/")
            .secure(secure)
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.STRICT)
            .build();
    }

    private NewCookie passwordResetCookie(String token, boolean secure) {
        return new NewCookie.Builder(PASSWORD_RESET_COOKIE_NAME)
            .value(token)
            .path("/")
            .maxAge((int) userSecurityConfiguration.getPasswordResetDuration().getSeconds())
            .secure(secure)
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.STRICT)
            .build();
    }

    static NewCookie clearSessionCookie(boolean secure) {
        return expiredCookie(SESSION_COOKIE_NAME, secure);
    }

    static NewCookie clearPasswordResetCookie(boolean secure) {
        return expiredCookie(PASSWORD_RESET_COOKIE_NAME, secure);
    }

    private static NewCookie expiredCookie(String name, boolean secure) {
        return new NewCookie.Builder(name)
            .value("")
            .path("/")
            .maxAge(0)
            .secure(secure)
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.STRICT)
            .build();
    }

    private static Response.ResponseBuilder noStore(Response.ResponseBuilder response) {
        return response.header("Cache-Control", "no-store").header("Pragma", "no-cache");
    }

    private static String sourceAddress(HttpServletRequest request) {
        return request == null ? null : request.getRemoteAddr();
    }
}
