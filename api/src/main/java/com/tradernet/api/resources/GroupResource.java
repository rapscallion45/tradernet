package com.tradernet.api.resources;

import com.tradernet.user.GroupManagementService;
import com.tradernet.user.dto.GroupDto;
import com.tradernet.user.dto.UpdateGroupRequestDto;
import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST API for querying groups.
 */
@Path("/groups")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GroupResource {

    @EJB
    private GroupManagementService groupManagementService;

    @GET
    public List<GroupDto> getGroups() {
        return groupManagementService.getGroups();
    }

    @GET
    @Path("/{id}")
    public Response getGroup(@Positive(message = "id must be greater than 0") @PathParam("id") long id) {
        return groupManagementService.getGroup(id)
            .map(group -> Response.ok(group).build())
            .orElseGet(() -> ApiErrors.response(Response.Status.NOT_FOUND, "Group not found"));
    }

    @PUT
    @Path("/{id}")
    public Response updateGroup(
        @Positive(message = "id must be greater than 0") @PathParam("id") long id,
        @NotNull(message = "Request body is required") @Valid UpdateGroupRequestDto request
    ) {
        return groupManagementService.updateGroup(id, request.getUsernames(), request.getRoleNames())
            .map(group -> Response.ok(group).build())
            .orElseGet(() -> ApiErrors.response(Response.Status.NOT_FOUND, "Group not found"));
    }
}
