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
    private OrderPresentationService orderPresentationService;

    @GET
    public Response getOrders(
        @Context SecurityContext securityContext,
        @QueryParam("userId") Long userId,
        @DefaultValue("USD") @QueryParam("currency") String currency
    ) {
        AuthUserDto authUser = AuthenticatedRequest.requireAuthenticatedUser(securityContext);
        long authenticatedUserId = authUser.getId();
        if (userId != null && userId != authenticatedUserId) {
            return ApiErrors.response(Response.Status.FORBIDDEN, "Cannot list orders for another user");
        }

        List<OrderResponseDto> response = orderPresentationService.getOrdersForUser(authenticatedUserId, currency);
        return Response.ok(response).build();
    }

    @POST
    public Response createOrder(@Context SecurityContext securityContext, @Valid OrderRequestDto request) {
        if (request == null) {
            return ApiErrors.response(Response.Status.BAD_REQUEST, "Order payload is required");
        }

        AuthUserDto authUser = AuthenticatedRequest.requireAuthenticatedUser(securityContext);

        String symbol = request.getSymbol();
        if (symbol == null || symbol.isBlank()) {
            return ApiErrors.response(Response.Status.BAD_REQUEST, "symbol is required");
        }

        if (request.getSide() == null) {
            return ApiErrors.response(Response.Status.BAD_REQUEST, "position is required");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            return ApiErrors.response(Response.Status.BAD_REQUEST, "quantity must be greater than 0");
        }

        if (request.getPrice() == null || request.getPrice() <= 0) {
            return ApiErrors.response(Response.Status.BAD_REQUEST, "price must be greater than 0");
        }

        return Response.status(Response.Status.CREATED)
            .entity(orderPresentationService.createOrder(authUser.getId(), request))
            .build();
    }

    @PUT
    @Path("/{orderId}/close")
    public Response closeOrder(
        @Context SecurityContext securityContext,
        @PathParam("orderId") Long orderId
    ) {
        AuthUserDto authUser = AuthenticatedRequest.requireAuthenticatedUser(securityContext);

        if (orderId == null || orderId <= 0) {
            return ApiErrors.response(Response.Status.BAD_REQUEST, "orderId must be greater than 0");
        }

        return orderPresentationService.closeOrder(authUser.getId(), orderId)
            .map(response -> Response.ok(response).build())
            .orElseGet(() -> ApiErrors.response(Response.Status.NOT_FOUND, "Order not found"));
    }
}
