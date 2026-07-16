package com.tradernet.api.resources;

import com.tradernet.user.UserService;
import com.tradernet.user.dto.UserProfileDto;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST API for querying users.
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @EJB
    private UserService userService;

    @GET
    public List<UserProfileDto> getUsers() {
        return userService.getUserProfiles();
    }

    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") long id) {
        return userService.getUserProfile(id)
            .map(user -> Response.ok(user).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/by-username/{username}")
    public Response getUserByUsername(@PathParam("username") String username) {
        return userService.getUserProfileByUsername(username)
            .map(user -> Response.ok(user).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
