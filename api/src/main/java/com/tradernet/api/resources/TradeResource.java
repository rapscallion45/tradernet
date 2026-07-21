package com.tradernet.api.resources;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.trade.TradeExecutionService;
import com.tradernet.trade.dto.TradeResponseDto;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.EJB;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

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
        @Context SecurityContext securityContext,
        @Pattern(regexp = MarketSymbolNormalizer.VALIDATION_PATTERN, message = "symbol is invalid")
        @QueryParam("symbol") String symbol
    ) {
        AuthUserDto authUser = AuthenticatedRequest.requireAuthenticatedUser(securityContext);
        List<TradeResponseDto> response = tradeExecutionService.getTradesForUser(authUser.getId(), symbol);
        return Response.ok(response).build();
    }
}
