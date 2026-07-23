package com.tradernet.api.resources;

import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.portfolio.PortfolioQueryService;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.EJB;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * REST API for viewing portfolio holdings and performance.
 */
@Path("/portfolio")
@Produces(MediaType.APPLICATION_JSON)
public class PortfolioResource {

    @EJB
    private PortfolioQueryService portfolioService;

    @GET
    public Response getPortfolio(
        @Context SecurityContext securityContext,
        @Pattern(regexp = CurrencyCode.VALIDATION_PATTERN, message = "currency is invalid")
        @DefaultValue("USD") @QueryParam("currency") String currency
    ) {
        AuthUserDto authUser = AuthenticatedRequest.requireAuthenticatedUser(securityContext);
        return Response.ok(portfolioService.getPortfolio(authUser.getId(), currency)).build();
    }
}
