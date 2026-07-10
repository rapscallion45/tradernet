package com.tradernet.api.resources;

import com.tradernet.api.resources.dto.TradeResponseDto;
import com.tradernet.trade.TradeExecutionService;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST API for querying trades.
 */
@Path("/trades")
@Produces(MediaType.APPLICATION_JSON)
public class TradeResource {

    @Inject
    private TradeExecutionService tradeExecutionService;

    @GET
    public Response getTrades(
        @CookieParam(AuthResource.SESSION_COOKIE_NAME) String sessionId,
        @QueryParam("symbol") String symbol
    ) {
        Optional<AuthUserDto> authUser = AuthResource.getSessionUser(sessionId);
        if (authUser.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("Not authenticated")
                .build();
        }

        List<TradeResponseDto> response = tradeExecutionService.getTradesForUser(authUser.get().getId(), symbol).stream()
            .map(TradeResponseDto::fromTrade)
            .collect(Collectors.toList());
        return Response.ok(response).build();
    }
}
