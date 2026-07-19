package com.tradernet.api.resources;

import com.tradernet.user.RoleManagementService;
import com.tradernet.user.dto.RoleDto;
import com.tradernet.user.dto.UpdateRoleRequestDto;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST API for querying roles.
 */
@Path("/roles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoleResource {

    @EJB
    private RoleManagementService roleManagementService;

    @GET
    public List<RoleDto> getRoles() {
        return roleManagementService.getRoles();
    }

    @GET
    @Path("/{name}")
    public Response getRole(@PathParam("name") String name) {
        return roleManagementService.getRole(name)
            .map(role -> Response.ok(role).build())
            .orElseGet(() -> ApiErrors.response(Response.Status.NOT_FOUND, "Role not found"));
    }

    @GET
    @Path("/resources")
    public List<String> getResources() {
        return roleManagementService.getResourceNames();
    }

    @PUT
    @Path("/{name}")
    public Response updateRole(@PathParam("name") String name, UpdateRoleRequestDto request) {
        if (request == null) {
            return ApiErrors.response(Response.Status.BAD_REQUEST, "Request body is required");
        }

        try {
            return roleManagementService.updateRole(name, request.getResourceNames())
                .map(role -> Response.ok(role).build())
                .orElseGet(() -> ApiErrors.response(Response.Status.NOT_FOUND, "Role not found"));
        } catch (IllegalArgumentException ex) {
            return ApiErrors.response(Response.Status.BAD_REQUEST, ex.getMessage());
        }
    }

}
