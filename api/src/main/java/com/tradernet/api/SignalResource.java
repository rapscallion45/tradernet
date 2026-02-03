package com.tradernet.api;

import com.tradernet.jpa.dao.SignalDao;
import com.tradernet.jpa.entities.SignalEntity;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * REST API for querying trading signals.
 */
@Path("/signals")
@Produces(MediaType.APPLICATION_JSON)
public class SignalResource {

    @EJB
    private SignalDao signalDao;

    @GET
    public List<SignalEntity> getSignals(@QueryParam("symbol") String symbol) {
        if (symbol != null && !symbol.isBlank()) {
            return signalDao.findBySymbol(symbol);
        }
        return signalDao.findAll();
    }
}
