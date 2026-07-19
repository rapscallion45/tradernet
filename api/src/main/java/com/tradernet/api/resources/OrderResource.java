package com.tradernet.api.resources;

import com.tradernet.order.OrderPresentationService;
import com.tradernet.order.dto.OrderRequestDto;
import com.tradernet.order.dto.OrderResponseDto;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST API for creating and listing persisted orders.
 */
@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @EJB
    private OrderPresentationService orderPresentationService;

    @GET
    public Response getOrders(
        @Context ContainerRequestContext request,
        @QueryParam("userId") Long userId,
        @DefaultValue("USD") @QueryParam("currency") String currency
    ) {
        AuthUserDto authUser = AuthenticatedRequest.requireAuthenticatedUser(request);
        long authenticatedUserId = authUser.getId();
        if (userId != null && userId != authenticatedUserId) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity("Cannot list orders for another user")
                .build();
        }

        List<OrderResponseDto> response = orderPresentationService.getOrdersForUser(authenticatedUserId, currency);
        return Response.ok(response).build();
    }

    @POST
    public Response createOrder(@Context ContainerRequestContext requestContext, @Valid OrderRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Order payload is required")
                .build();
        }

        AuthUserDto authUser = AuthenticatedRequest.requireAuthenticatedUser(requestContext);

        String symbol = request.getSymbol();
        if (symbol == null || symbol.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("symbol is required")
                .build();
        }

        if (request.getSide() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("position is required")
                .build();
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("quantity must be greater than 0")
                .build();
        }

        if (request.getPrice() == null || request.getPrice() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("price must be greater than 0")
                .build();
        }

        return Response.status(Response.Status.CREATED)
            .entity(orderPresentationService.createOrder(authUser.getId(), request))
            .build();
    }

    @PUT
    @Path("/{orderId}/close")
    public Response closeOrder(
        @Context ContainerRequestContext request,
        @PathParam("orderId") Long orderId
    ) {
        AuthUserDto authUser = AuthenticatedRequest.requireAuthenticatedUser(request);

        if (orderId == null || orderId <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("orderId must be greater than 0")
                .build();
        }

        return orderPresentationService.closeOrder(authUser.getId(), orderId)
            .map(response -> Response.ok(response).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                .entity("Order not found")
                .build());
    }
}
