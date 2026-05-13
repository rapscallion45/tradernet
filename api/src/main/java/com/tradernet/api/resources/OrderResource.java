package com.tradernet.api.resources;

import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.currencyconversion.CurrencyConversionService;
import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.model.AiSignal;
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
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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

    @Inject
    private CurrencyConversionService currencyConversionService;

    @GET
    public Response getOrders(
        @CookieParam(AuthResource.SESSION_COOKIE_NAME) String sessionId,
        @QueryParam("userId") Long userId,
        @DefaultValue("USD") @QueryParam("currency") String currency
    ) {
        List<OrderEntity> orders;
        if (userId != null && userId > 0) {
            orders = orderService.getOrdersByUserId(userId);
        } else {
            Optional<AuthUserDto> authUser = AuthResource.getSessionUser(sessionId);
            orders = authUser.map(user -> orderService.getOrdersByUserId(user.getId())).orElseGet(orderService::getOrders);
        }

        List<OrderResponseDto> response = orders.stream()
                        .map(order -> toResponse(order, CurrencyCode.parseOrDefault(currency, CurrencyCode.USD)))
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

        OrderEntity order = new OrderEntity(symbol.trim(), request.getQuantity(), request.getPrice(), side);
        order.setAiPrediction(resolveAiPrediction(symbol));
        OrderEntity savedOrder = orderService.createOrder(authUser.get().getId(), order);

        return Response.status(Response.Status.CREATED)
            .entity(toResponse(savedOrder, CurrencyCode.USD))
            .build();
    }

    @PUT
    @Path("/{orderId}/close")
    public Response closeOrder(
        @CookieParam(AuthResource.SESSION_COOKIE_NAME) String sessionId,
        @PathParam("orderId") Long orderId
    ) {
        Optional<AuthUserDto> authUser = AuthResource.getSessionUser(sessionId);
        if (authUser.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("Not authenticated")
                .build();
        }

        if (orderId == null || orderId <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("orderId must be greater than 0")
                .build();
        }

        Optional<OrderEntity> targetOrder = orderService.getOrderForUser(authUser.get().getId(), orderId);
        if (targetOrder.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("Order not found")
                .build();
        }

        OrderEntity order = targetOrder.get();
        double closePrice = resolveCurrentPrice(order.getSymbol(), order.getPrice());
        OrderEntity closedOrder = orderService.closeOrder(authUser.get().getId(), orderId, closePrice).orElse(order);

        return Response.ok(toResponse(closedOrder, CurrencyCode.USD)).build();
    }

    private OrderResponseDto toResponse(OrderEntity order, CurrencyCode displayCurrency) {
        OrderResponseDto responseDto = OrderResponseDto.fromOrder(order);

        boolean closed = OrderService.CLOSED_STATUS.equals(order.getStatus()) && order.getClosePrice() != null;
        CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(order.getSymbol());

        double rawCurrentPrice = closed
            ? order.getClosePrice()
            : resolveCurrentPrice(order.getSymbol(), order.getPrice());
        double rawEntry = order.getPrice();
        double quantity = order.getQuantity();

        Instant entryTimestamp = order.getCreatedAt() == null ? Instant.now() : order.getCreatedAt();
        Instant currentTimestamp = closed && order.getClosedAt() != null ? order.getClosedAt() : Instant.now();

        double entry = currencyConversionService.convertAmount(rawEntry, sourceCurrency, displayCurrency, entryTimestamp);
        double currentPrice = currencyConversionService.convertAmount(rawCurrentPrice, sourceCurrency, displayCurrency, currentTimestamp);
        Double closePrice = order.getClosePrice() == null ? null
            : currencyConversionService.convertAmount(order.getClosePrice(), sourceCurrency, displayCurrency, currentTimestamp);

        double rawPnlPerUnit = order.getSide() == OrderEntity.Side.BUY ? (rawCurrentPrice - rawEntry) : (rawEntry - rawCurrentPrice);
        double pnlPerUnit = currencyConversionService.convertAmount(rawPnlPerUnit, sourceCurrency, displayCurrency, currentTimestamp);
        double pnl = pnlPerUnit * quantity;
        double pnlPercent = rawEntry == 0 ? 0 : (rawPnlPerUnit / rawEntry) * 100.0;
        double netValue = currentPrice * quantity;

        responseDto.setPrice(roundCurrency(entry));
        responseDto.setCurrentPrice(roundCurrency(currentPrice));
        responseDto.setPnl(roundCurrency(pnl));
        responseDto.setPnlPercent(roundCurrency(pnlPercent));
        responseDto.setNetValue(roundCurrency(netValue));
        responseDto.setTiming(closed ? "CLOSED" : (pnlPerUnit > 0 ? "GOOD" : (pnlPerUnit < 0 ? "BAD" : "NEUTRAL")));
        responseDto.setClosedAt(order.getClosedAt());
        responseDto.setClosePrice(closePrice == null ? null : roundCurrency(closePrice));

        responseDto.setCreatedAtDisplay(responseDto.getCreatedAt() == null ? "" : responseDto.getCreatedAt().toString());
        responseDto.setCurrentPriceDisplay(String.format(Locale.US, "%.2f", responseDto.getCurrentPrice()));
        responseDto.setPnlDisplay(String.format(Locale.US, "%.2f", responseDto.getPnl()));
        responseDto.setPnlPercentDisplay(String.format(Locale.US, "%.2f%%", responseDto.getPnlPercent()));
        responseDto.setNetValueDisplay(String.format(Locale.US, "%.2f", responseDto.getNetValue()));

        return responseDto;
    }


    private double roundCurrency(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String resolveAiPrediction(String symbol) {
        List<AiSignal> signals = marketAiService.getSignals(200);
        if (signals == null || signals.isEmpty()) {
            return "HOLD";
        }

        String normalizedSymbol = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);

        return signals.stream()
            .filter(signal -> signal != null && signal.getSide() != null)
            .filter(signal -> normalizedSymbol.isEmpty() || (signal.getSymbol() != null && signal.getSymbol().equalsIgnoreCase(normalizedSymbol)))
            .max(java.util.Comparator.comparingLong(AiSignal::getEventTime))
            .map(signal -> signal.getSide().name())
            .orElse("HOLD");
    }

    private double resolveCurrentPrice(String symbol, double fallbackPrice) {
        List<MarketBar> bars = marketAiService.getBars(symbol, "1S", 1);
        if (bars == null || bars.isEmpty() || bars.get(0) == null) {
            return fallbackPrice;
        }
        return bars.get(0).getClose();
    }
}
