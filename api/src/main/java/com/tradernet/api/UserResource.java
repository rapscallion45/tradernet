package com.tradernet.api;

import com.tradernet.jpa.dao.UserDao;
import com.tradernet.jpa.entities.UserEntity;
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
    private UserDao userDao;

    @GET
    public List<UserEntity> getUsers() {
        return userDao.findAll();
    }

    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") long id) {
        return userDao.findById(id)
            .map(user -> Response.ok(user).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/by-username/{username}")
    public Response getUserByUsername(@PathParam("username") String username) {
        return userDao.findByUsername(username)
            .map(user -> Response.ok(user).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
