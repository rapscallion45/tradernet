package com.tradernet.api.resources;

import com.tradernet.jpa.dao.UserPropertyDao;
import com.tradernet.jpa.entities.UserPropertyEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * REST API for querying user properties.
 */
@Path("/user-properties")
@Produces(MediaType.APPLICATION_JSON)
public class UserPropertyResource {

    @Inject
    private UserPropertyDao userPropertyDao;

    @GET
    public List<UserPropertyEntity> getUserProperties(@QueryParam("userId") Long userId) {
        if (userId != null && userId > 0) {
            return userPropertyDao.findByUserId(userId);
        }
        return userPropertyDao.findAll();
    }
}
