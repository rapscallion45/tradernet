package com.tradernet.order;

import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.currencyconversion.CurrencyConversionService;
import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.MarketSymbolNormalizer;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.order.dto.OrderRequestDto;
import com.tradernet.order.dto.OrderResponseDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.tradernet.order.CurrencyRounding.roundCurrency;

/**
 * Builds user-scoped order responses with market, forecast, and currency enrichment.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OrderPresentationService {

    private static final int DEFAULT_ORDER_BULL_SCORE_HORIZON_DAYS = 1;

    @EJB
    private OrderService orderService;

    @EJB
    private MarketAiService marketAiService;

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
        final OrderEntity order = new OrderEntity(symbol, request.getQuantity(), request.getPrice(), request.getSide());
        order.setAiPrediction(resolveAiPrediction(symbol));
        order.setBullScore(resolveBullScore(symbol));
        return toResponse(orderService.createOrder(userId, order), CurrencyCode.USD);
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
        final OrderResponseDto responseDto = OrderResponseDto.fromOrder(order);

        final boolean closed = OrderService.CLOSED_STATUS.equals(order.getStatus()) && order.getClosePrice() != null;
        final CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(order.getSymbol());

        final double rawCurrentPrice = closed
            ? order.getClosePrice()
            : marketPriceService.resolveCurrentPrice(order.getSymbol(), order.getPrice());
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

        responseDto.setCreatedAtDisplay(responseDto.getCreatedAt() == null ? "" : responseDto.getCreatedAt().toString());
        responseDto.setCurrentPriceDisplay(String.format(Locale.US, "%.2f", responseDto.getCurrentPrice()));
        responseDto.setPnlDisplay(String.format(Locale.US, "%.2f", responseDto.getPnl()));
        responseDto.setPnlPercentDisplay(String.format(Locale.US, "%.2f%%", responseDto.getPnlPercent()));
        responseDto.setNetValueDisplay(String.format(Locale.US, "%.2f", responseDto.getNetValue()));

        return responseDto;
    }

    private Double resolveBullScore(String symbol) {
        final int horizonDays = Integer.parseInt(System.getProperty(
            "market.ai.orderBullScoreHorizonDays",
            String.valueOf(DEFAULT_ORDER_BULL_SCORE_HORIZON_DAYS)
        ));
        return roundCurrency(marketAiService.getBullScore(symbol, horizonDays));
    }

    private String resolveAiPrediction(String symbol) {
        final List<AiSignal> signals = marketAiService.getSignals(symbol, 200);
        if (signals == null || signals.isEmpty()) {
            return "HOLD";
        }

        return signals.stream()
            .filter(signal -> signal != null && signal.getSide() != null)
            .max(java.util.Comparator.comparingLong(AiSignal::getEventTime))
            .map(signal -> signal.getSide().name())
            .orElse("HOLD");
    }

}
