package com.tradernet.api.resources;

import com.tradernet.order.PortfolioService;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.EJB;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;

/**
 * REST API for viewing portfolio holdings and performance.
 */
@Path("/portfolio")
@Produces(MediaType.APPLICATION_JSON)
public class PortfolioResource {

    @EJB
    private PortfolioService portfolioService;

    @GET
    public Response getPortfolio(
        @Context ContainerRequestContext request,
        @DefaultValue("USD") @QueryParam("currency") String currency
    ) {
        Optional<AuthUserDto> authUser = AuthenticatedRequest.authenticatedUser(request);
        if (authUser.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("Not authenticated")
                .build();
        }

        return Response.ok(portfolioService.getPortfolio(authUser.get().getId(), currency)).build();
    }
}
