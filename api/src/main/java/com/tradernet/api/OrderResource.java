package com.tradernet.api;

import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.order.OrderService;
import com.tradernet.order.dto.OrderRequestDto;
import com.tradernet.order.dto.OrderResponseDto;
import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for creating and listing orders.
 */
@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @EJB
    private OrderService orderService;

    @GET
    public List<OrderResponseDto> getOrders() {
        return orderService.getOrders().stream()
            .map(OrderResponseDto::fromOrder)
            .collect(Collectors.toList());
    }

    @POST
    public Response createOrder(@Valid OrderRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Order payload is required")
                .build();
        }

        String symbol = request.getSymbol();
        if (symbol == null || symbol.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("symbol is required")
                .build();
        }

        OrderEntity.Side side = request.getSide();
        if (side == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("side is required")
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

        OrderEntity order = new OrderEntity(symbol.trim(), request.getQuantity().intValue(), request.getPrice(), side);
        orderService.createOrder(order);

        return Response.status(Response.Status.CREATED)
            .entity(OrderResponseDto.fromOrder(order))
            .build();
    }
}
