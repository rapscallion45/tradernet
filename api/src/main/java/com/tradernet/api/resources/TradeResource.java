package com.tradernet.api.resources;

import com.tradernet.jpa.dao.TradeDao;
import com.tradernet.jpa.entities.TradeEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * REST API for querying trades.
 */
@Path("/trades")
@Produces(MediaType.APPLICATION_JSON)
public class TradeResource {

    @Inject
    private TradeDao tradeDao;

    @GET
    public List<TradeEntity> getTrades(@QueryParam("symbol") String symbol) {
        if (symbol != null && !symbol.isBlank()) {
            return tradeDao.findBySymbol(symbol);
        }
        return tradeDao.findAll();
    }
}
