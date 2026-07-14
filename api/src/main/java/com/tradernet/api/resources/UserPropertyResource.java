package com.tradernet.api.resources;

import com.tradernet.api.resources.dto.UserPropertyDto;
import com.tradernet.jpa.dao.UserPropertyDao;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for querying user properties.
 */
@Path("/user-properties")
@Produces(MediaType.APPLICATION_JSON)
public class UserPropertyResource {

    @EJB
    private UserPropertyDao userPropertyDao;

    @GET
    public List<UserPropertyDto> getUserProperties(@QueryParam("userId") Long userId) {
        if (userId != null && userId > 0) {
            return userPropertyDao.findByUserId(userId).stream()
                .map(UserPropertyDto::fromEntity)
                .collect(Collectors.toList());
        }
        return userPropertyDao.findAll().stream()
            .map(UserPropertyDto::fromEntity)
            .collect(Collectors.toList());
    }
}
