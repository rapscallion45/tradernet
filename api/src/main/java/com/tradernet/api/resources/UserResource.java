package com.tradernet.api.resources;

import com.tradernet.user.UserProfileQueryService;
import com.tradernet.user.dto.UserProfileDto;
import jakarta.ejb.EJB;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    private UserProfileQueryService userProfileService;

    @GET
    public List<UserProfileDto> getUsers() {
        return userProfileService.getUserProfiles();
    }

    @GET
    @Path("/{id}")
    public Response getUser(@Positive(message = "id must be greater than 0") @PathParam("id") long id) {
        return userProfileService.getUserProfile(id)
            .map(user -> Response.ok(user).build())
            .orElseGet(() -> ApiErrors.response(Response.Status.NOT_FOUND, "User not found"));
    }

    @GET
    @Path("/by-username/{username}")
    public Response getUserByUsername(
        @NotBlank(message = "username is required") @Size(max = 100) @PathParam("username") String username
    ) {
        return userProfileService.getUserProfileByUsername(username)
            .map(user -> Response.ok(user).build())
            .orElseGet(() -> ApiErrors.response(Response.Status.NOT_FOUND, "User not found"));
    }
}
