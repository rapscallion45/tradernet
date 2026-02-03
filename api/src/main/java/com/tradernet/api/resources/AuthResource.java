package com.tradernet.api.resources;

import com.tradernet.user.dto.AuthUserDto;
import com.tradernet.user.dto.ForgotPasswordRequestDto;
import com.tradernet.user.dto.LoginRequestDto;
import com.tradernet.user.dto.LoginResponseDto;
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

    private static final String SESSION_COOKIE_NAME = "tradernet_session";
    private static final Duration SESSION_DURATION = Duration.ofHours(8);
    private static final Map<String, AuthUserDto> SESSIONS = new ConcurrentHashMap<>();

    @EJB
    private UserService userService;

    @POST
    @Path("/login")
    public Response login(LoginRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new MessageResponseDto("Login payload is required"))
                .build();
        }

        String username = request.getUsername();
        String password = request.getPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new MessageResponseDto("username and password are required"))
                .build();
        }

        if (!userService.authenticate(username, password)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new MessageResponseDto("Invalid credentials"))
                .build();
        }

        Optional<UserEntity> user = userService.findByUsername(username);
        if (user.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new MessageResponseDto("User not found"))
                .build();
        }

        String token = UUID.randomUUID().toString();
        AuthUserDto authUser = AuthUserDto.fromUser(user.get());
        SESSIONS.put(token, authUser);

        LoginResponseDto response = new LoginResponseDto(authUser);
        NewCookie sessionCookie = new NewCookie.Builder(SESSION_COOKIE_NAME)
            .value(token)
            .path("/")
            .maxAge((int) SESSION_DURATION.getSeconds())
            .httpOnly(true)
            .build();

        return Response.ok(response)
            .cookie(sessionCookie)
            .build();
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam(SESSION_COOKIE_NAME) String sessionId) {
        if (sessionId != null) {
            SESSIONS.remove(sessionId);
        }
        NewCookie clearCookie = new NewCookie.Builder(SESSION_COOKIE_NAME)
            .value("")
            .path("/")
            .maxAge(0)
            .httpOnly(true)
            .build();
        return Response.ok(new MessageResponseDto("Logged out"))
            .cookie(clearCookie)
            .build();
    }

    @GET
    @Path("/session")
    public Response getSession(@CookieParam(SESSION_COOKIE_NAME) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new MessageResponseDto("Not authenticated"))
                .build();
        }

        AuthUserDto user = SESSIONS.get(sessionId);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new MessageResponseDto("Session expired"))
                .build();
        }

        return Response.ok(user).build();
    }

    @POST
    @Path("/forgot-password")
    public Response forgotPassword(ForgotPasswordRequestDto request) {
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

        try {
            userService.resetPassword(username, newPassword);
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new MessageResponseDto(ex.getMessage()))
                .build();
        }

        return Response.ok(new MessageResponseDto("Password reset"))
            .build();
    }
}
