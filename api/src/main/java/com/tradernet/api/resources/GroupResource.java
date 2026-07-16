package com.tradernet.api.resources;

import com.tradernet.user.GroupManagementService;
import com.tradernet.user.dto.GroupDto;
import com.tradernet.user.dto.UpdateGroupRequestDto;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.BadRequestException;
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
    public Response getGroup(@PathParam("id") long id) {
        return groupManagementService.getGroup(id)
            .map(group -> Response.ok(group).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Path("/{id}")
    public Response updateGroup(@PathParam("id") long id, UpdateGroupRequestDto request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }

        try {
            return groupManagementService.updateGroup(id, request.getUsernames(), request.getRoleNames())
                .map(group -> Response.ok(group).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }
}
