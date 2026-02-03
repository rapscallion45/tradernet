package com.tradernet.api.resources;

import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.entities.RoleEntity;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
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
@Produces(MediaType.APPLICATION_JSON)
public class RoleResource {

    @EJB
    private RoleDao roleDao;

    @GET
    public List<RoleEntity> getRoles() {
        return roleDao.findAll();
    }

    @GET
    @Path("/{name}")
    public Response getRole(@PathParam("name") String name) {
        return roleDao.findByName(name)
            .map(role -> Response.ok(role).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
