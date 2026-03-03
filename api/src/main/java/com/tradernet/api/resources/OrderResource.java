package com.tradernet.api.resources;

import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.order.OrderService;
import com.tradernet.order.dto.OrderRequestDto;
import com.tradernet.order.dto.OrderResponseDto;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST API for creating and listing persisted mock orders.
 */
@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    private OrderService orderService;

    @Inject
    private MarketAiService marketAiService;

    @GET
    public Response getOrders(
        @CookieParam(AuthResource.SESSION_COOKIE_NAME) String sessionId,
        @QueryParam("userId") Long userId
    ) {
        List<OrderEntity> orders;
        if (userId != null && userId > 0) {
            orders = orderService.getOrdersByUserId(userId);
        } else {
            Optional<AuthUserDto> authUser = AuthResource.getSessionUser(sessionId);
            orders = authUser.map(user -> orderService.getOrdersByUserId(user.getId())).orElseGet(orderService::getOrders);
        }

        List<OrderResponseDto> response = orders.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return Response.ok(response).build();
    }

    @POST
    public Response createOrder(@CookieParam(AuthResource.SESSION_COOKIE_NAME) String sessionId, @Valid OrderRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Order payload is required")
                .build();
        }

        Optional<AuthUserDto> authUser = AuthResource.getSessionUser(sessionId);
        if (authUser.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("Not authenticated")
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

        OrderEntity order = new OrderEntity(symbol.trim(), request.getQuantity(), request.getPrice(), side);
        OrderEntity savedOrder = orderService.createOrder(authUser.get().getId(), order);

        return Response.status(Response.Status.CREATED)
            .entity(toResponse(savedOrder))
            .build();
    }

    private OrderResponseDto toResponse(OrderEntity order) {
        OrderResponseDto responseDto = OrderResponseDto.fromOrder(order);

        double currentPrice = resolveCurrentPrice(order.getSymbol(), order.getPrice());
        double entry = order.getPrice();
        double quantity = order.getQuantity();

        double pnlPerUnit = order.getSide() == OrderEntity.Side.BUY ? (currentPrice - entry) : (entry - currentPrice);
        double pnl = pnlPerUnit * quantity;
        double pnlPercent = entry == 0 ? 0 : (pnlPerUnit / entry) * 100.0;

        responseDto.setCurrentPrice(currentPrice);
        responseDto.setPnl(pnl);
        responseDto.setPnlPercent(pnlPercent);
        responseDto.setTiming(pnlPerUnit > 0 ? "GOOD" : (pnlPerUnit < 0 ? "BAD" : "NEUTRAL"));

        responseDto.setCreatedAtDisplay(responseDto.getCreatedAt() == null ? "" : responseDto.getCreatedAt().toString());
        responseDto.setCurrentPriceDisplay(String.format(Locale.US, "%.4f", currentPrice));
        responseDto.setPnlDisplay(String.format(Locale.US, "%.4f", pnl));
        responseDto.setPnlPercentDisplay(String.format(Locale.US, "%.2f%%", pnlPercent));

        return responseDto;
    }

    private double resolveCurrentPrice(String symbol, double fallbackPrice) {
        List<MarketBar> bars = marketAiService.getBars(symbol, "1S", 1);
        if (bars == null || bars.isEmpty() || bars.get(0) == null) {
            return fallbackPrice;
        }
        return bars.get(0).getClose();
    }
}
