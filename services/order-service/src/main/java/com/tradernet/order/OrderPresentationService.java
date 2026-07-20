package com.tradernet.order;

import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.currencyconversion.CurrencyConversionService;
import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.marketai.MarketSymbolNormalizer;
import com.tradernet.order.dto.OrderRequestDto;
import com.tradernet.order.dto.OrderResponseDto;
import com.tradernet.order.dto.OrderSide;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.tradernet.order.CurrencyRounding.roundCurrency;

/**
 * Builds user-scoped order responses with market, forecast, and currency enrichment.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OrderPresentationService {

    @EJB
    private OrderService orderService;

    @EJB
    private OrderInsightEnrichmentService orderInsightEnrichmentService;

    @EJB
    private MarketPriceService marketPriceService;

    @EJB
    private CurrencyConversionService currencyConversionService;

    public List<OrderResponseDto> getOrdersForUser(long userId, String currency) {
        final CurrencyCode displayCurrency = CurrencyCode.parseOrDefault(currency, CurrencyCode.USD);
        return orderService.getOrdersByUserId(userId).stream()
            .map(order -> toResponse(order, displayCurrency))
            .collect(Collectors.toList());
    }

    public OrderResponseDto createOrder(long userId, OrderRequestDto request) {
        final String symbol = MarketSymbolNormalizer.normalizeSymbol(request.getSymbol());
        final OrderEntity order = new OrderEntity(symbol, request.getQuantity(), request.getPrice(), toEntitySide(request.getSide()));
        final OrderEntity created = orderService.createOrder(userId, order);
        orderInsightEnrichmentService.enrichOrder(created.getId(), symbol);
        return toResponse(created, CurrencyCode.USD, false);
    }

    public Optional<OrderResponseDto> closeOrder(long userId, long orderId) {
        final Optional<OrderEntity> targetOrder = orderService.getOrderForUser(userId, orderId);
        if (targetOrder.isEmpty()) {
            return Optional.empty();
        }

        final OrderEntity order = targetOrder.get();
        final double closePrice = marketPriceService.resolveCurrentPrice(order.getSymbol(), order.getPrice());
        final OrderEntity closedOrder = orderService.closeOrder(userId, orderId, closePrice).orElse(order);
        return Optional.of(toResponse(closedOrder, CurrencyCode.USD));
    }

    private OrderResponseDto toResponse(OrderEntity order, CurrencyCode displayCurrency) {
        return toResponse(order, displayCurrency, true);
    }

    private OrderResponseDto toResponse(OrderEntity order, CurrencyCode displayCurrency, boolean resolveLivePricing) {
        final OrderResponseDto responseDto = new OrderResponseDto();
        final long resolvedId = order.getId() == null ? 0L : order.getId();
        responseDto.setId(resolvedId);
        responseDto.setOrderId(resolvedId);
        responseDto.setUserId(order.getUserId() == null ? 0L : order.getUserId());
        responseDto.setSymbol(order.getSymbol());
        responseDto.setSide(order.getSide() == null ? null : order.getSide().name());
        responseDto.setCurrency(displayCurrency.name());
        responseDto.setQuantity(order.getQuantity());
        responseDto.setPrice(order.getPrice());
        responseDto.setStatus(order.getStatus());
        responseDto.setCreatedAt(order.getCreatedAt());
        responseDto.setClosedAt(order.getClosedAt());
        responseDto.setAiPrediction(order.getAiPrediction());
        responseDto.setBullScore(order.getBullScore());
        responseDto.setClosePrice(order.getClosePrice());

        final boolean closed = OrderService.CLOSED_STATUS.equals(order.getStatus()) && order.getClosePrice() != null;
        final CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(order.getSymbol());

        final double rawCurrentPrice;
        if (closed) {
            rawCurrentPrice = order.getClosePrice();
        } else if (resolveLivePricing) {
            rawCurrentPrice = marketPriceService.resolveCurrentPrice(order.getSymbol(), order.getPrice());
        } else {
            rawCurrentPrice = order.getPrice();
        }
        final double rawEntry = order.getPrice();
        final double quantity = order.getQuantity();

        final Instant entryTimestamp = order.getCreatedAt() == null ? Instant.now() : order.getCreatedAt();
        final Instant currentTimestamp = closed && order.getClosedAt() != null ? order.getClosedAt() : Instant.now();

        final double entry = currencyConversionService.convertAmount(rawEntry, sourceCurrency, displayCurrency, entryTimestamp);
        final double currentPrice = currencyConversionService.convertAmount(rawCurrentPrice, sourceCurrency, displayCurrency, currentTimestamp);
        final Double closePrice = order.getClosePrice() == null ? null
            : currencyConversionService.convertAmount(order.getClosePrice(), sourceCurrency, displayCurrency, currentTimestamp);

        final double rawPnlPerUnit = order.getSide() == OrderEntity.Side.BUY ? (rawCurrentPrice - rawEntry) : (rawEntry - rawCurrentPrice);
        final double pnlPerUnit = currencyConversionService.convertAmount(rawPnlPerUnit, sourceCurrency, displayCurrency, currentTimestamp);
        final double pnl = pnlPerUnit * quantity;
        final double pnlPercent = rawEntry == 0 ? 0 : (rawPnlPerUnit / rawEntry) * 100.0;
        final double netValue = currentPrice * quantity;

        responseDto.setPrice(roundCurrency(entry));
        responseDto.setCurrentPrice(roundCurrency(currentPrice));
        responseDto.setPnl(roundCurrency(pnl));
        responseDto.setPnlPercent(roundCurrency(pnlPercent));
        responseDto.setNetValue(roundCurrency(netValue));
        responseDto.setTiming(closed ? "CLOSED" : (pnlPerUnit > 0 ? "GOOD" : (pnlPerUnit < 0 ? "BAD" : "NEUTRAL")));
        responseDto.setClosedAt(order.getClosedAt());
        responseDto.setClosePrice(closePrice == null ? null : roundCurrency(closePrice));

        return responseDto;
    }

    private OrderEntity.Side toEntitySide(OrderSide side) {
        if (side == null) {
            throw new IllegalArgumentException("order side is required");
        }
        return OrderEntity.Side.valueOf(side.name());
    }

}
