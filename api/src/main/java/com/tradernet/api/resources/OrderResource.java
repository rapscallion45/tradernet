package com.tradernet.api.resources;

import com.tradernet.order.OrderApplicationService;
import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.order.dto.OrderRequestDto;
import com.tradernet.order.dto.OrderResponseDto;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

/**
 * REST API for creating and listing persisted orders.
 */
@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @EJB
    private OrderApplicationService orderPresentationService;

    @GET
    public Response getOrders(
        @Context SecurityContext securityContext,
        @Pattern(regexp = CurrencyCode.VALIDATION_PATTERN, message = "currency is invalid")
        @DefaultValue("USD") @QueryParam("currency") String currency
    ) {
        AuthUserDto authUser = AuthenticatedRequest.requireAuthenticatedUser(securityContext);
        long authenticatedUserId = authUser.getId();

        List<OrderResponseDto> response = orderPresentationService.getOrdersForUser(authenticatedUserId, currency);
        return Response.ok(response).build();
    }

    @POST
    public Response createOrder(
        @Context SecurityContext securityContext,
        @NotNull(message = "Order payload is required") @Valid OrderRequestDto request
    ) {
        AuthUserDto authUser = AuthenticatedRequest.requireAuthenticatedUser(securityContext);

        return Response.status(Response.Status.CREATED)
            .entity(orderPresentationService.createOrder(authUser.getId(), request))
            .build();
    }

    @PUT
    @Path("/{orderId}/close")
    public Response closeOrder(
        @Context SecurityContext securityContext,
        @Positive(message = "orderId must be greater than 0") @PathParam("orderId") long orderId
    ) {
        AuthUserDto authUser = AuthenticatedRequest.requireAuthenticatedUser(securityContext);

        return orderPresentationService.closeOrder(authUser.getId(), orderId)
            .map(response -> Response.ok(response).build())
            .orElseGet(() -> ApiErrors.response(Response.Status.NOT_FOUND, "Order not found"));
    }
}
