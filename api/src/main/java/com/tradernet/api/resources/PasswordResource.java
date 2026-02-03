package com.tradernet.api.resources;

import com.tradernet.jpa.dao.PasswordDao;
import com.tradernet.jpa.entities.PasswordEntity;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * REST API for querying password records.
 */
@Path("/passwords")
@Produces(MediaType.APPLICATION_JSON)
public class PasswordResource {

    @EJB
    private PasswordDao passwordDao;

    @GET
    public List<PasswordEntity> getPasswords(@QueryParam("userId") Long userId) {
        if (userId != null && userId > 0) {
            return passwordDao.findByUserId(userId);
        }
        return passwordDao.findAll();
    }
}
