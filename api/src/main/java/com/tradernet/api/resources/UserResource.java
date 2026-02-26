package com.tradernet.api.resources;

import com.tradernet.user.UserService;
import com.tradernet.user.dto.UserProfileDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for querying users.
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    private UserService userService;

    @GET
    public List<UserProfileDto> getUsers() {
        return userService.findAllWithRoles()
            .stream()
            .map(UserProfileDto::fromUser)
            .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") long id) {
        return userService.findByIdWithRoles(id)
            .map(user -> Response.ok(UserProfileDto.fromUser(user)).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/by-username/{username}")
    public Response getUserByUsername(@PathParam("username") String username) {
        return userService.findByUsernameWithRoles(username)
            .map(user -> Response.ok(UserProfileDto.fromUser(user)).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
