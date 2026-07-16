package com.tradernet.api.resources;

import com.tradernet.trade.TradeExecutionService;
import com.tradernet.trade.dto.TradeResponseDto;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

/**
 * REST API for querying trades.
 */
@Path("/trades")
@Produces(MediaType.APPLICATION_JSON)
public class TradeResource {

    @EJB
    private TradeExecutionService tradeExecutionService;

    @GET
    public Response getTrades(
        @Context ContainerRequestContext request,
        @QueryParam("symbol") String symbol
    ) {
        Optional<AuthUserDto> authUser = AuthenticatedRequest.authenticatedUser(request);
        if (authUser.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("Not authenticated")
                .build();
        }

        List<TradeResponseDto> response = tradeExecutionService.getTradesForUser(authUser.get().getId(), symbol);
        return Response.ok(response).build();
    }
}
