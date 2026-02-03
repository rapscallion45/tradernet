package com.tradernet.api;

import com.tradernet.api.dto.AuthUserDto;
import com.tradernet.api.dto.ForgotPasswordRequestDto;
import com.tradernet.api.dto.LoginRequestDto;
import com.tradernet.api.dto.LoginResponseDto;
import com.tradernet.api.dto.MessageResponseDto;
import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.user.UserService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;
import java.util.UUID;

/**
 * REST API for authentication workflows.
 */
@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

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
        LoginResponseDto response = new LoginResponseDto(token, AuthUserDto.fromUser(user.get()));

        return Response.ok(response).build();
    }

    @POST
    @Path("/logout")
    public Response logout() {
        return Response.ok(new MessageResponseDto("Logged out"))
            .build();
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
