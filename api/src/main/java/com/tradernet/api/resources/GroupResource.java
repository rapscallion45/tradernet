package com.tradernet.api.resources;

import com.tradernet.jpa.dao.GroupDao;
import com.tradernet.jpa.entities.GroupEntity;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST API for querying groups.
 */
@Path("/groups")
@Produces(MediaType.APPLICATION_JSON)
public class GroupResource {

    @EJB
    private GroupDao groupDao;

    @GET
    public List<GroupEntity> getGroups() {
        return groupDao.findAll();
    }

    @GET
    @Path("/{id}")
    public Response getGroup(@PathParam("id") long id) {
        return groupDao.findById(id)
            .map(group -> Response.ok(group).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
