package com.tradernet.order;

import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.currencyconversion.CurrencyConversionProvider;
import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.order.dto.OrderRequestDto;
import com.tradernet.order.dto.OrderResponseDto;
import com.tradernet.order.dto.OrderSide;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.tradernet.order.CurrencyRounding.roundCurrency;

/**
 * Builds user-scoped order responses with market and currency enrichment.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OrderPresentationService implements OrderApplicationService {

    @EJB
    private OrderCommandService orderCommands;

    @EJB
    private OrderQueryService orderQueries;

    @EJB
    private OrderInsightEnrichmentService orderInsightEnrichmentService;

    @EJB
    private MarketPriceService marketPriceService;

    @EJB
    private CurrencyConversionProvider currencyConversionService;

    @Override
    public List<OrderResponseDto> getOrdersForUser(long userId, String currency) {
        final CurrencyCode displayCurrency = CurrencyCode.parseOrDefault(currency, CurrencyCode.USD);
        final List<OrderRecord> orders = orderQueries.getOrdersByUserId(userId);
        prefetchOrderRates(orders, displayCurrency);

        final Map<String, Double> cachedPrices = new HashMap<>();
        for (OrderRecord order : orders) {
            if (order.getStatus() != OrderStatus.CLOSED) {
                marketPriceService.getLatestCachedPrice(order.getSymbol())
                    .ifPresent(price -> cachedPrices.putIfAbsent(order.getSymbol(), price));
            }
        }
        return orders.stream()
            .map(order -> toResponse(order, displayCurrency, cachedPrices.get(order.getSymbol())))
            .collect(Collectors.toList());
    }

    @Override
    public OrderResponseDto createOrder(long userId, OrderRequestDto request) {
        final String symbol = MarketSymbolNormalizer.normalizeSymbol(request.getSymbol());
        final OrderRecord created = orderCommands.createOrder(
            userId,
            new CreateOrderCommand(symbol, request.getQuantity(), request.getPrice(), request.getSide())
        );
        orderInsightEnrichmentService.enrichOrder(created.getId(), symbol);
        return toResponse(created, CurrencyCode.USD, null);
    }

    @Override
    public Optional<OrderResponseDto> closeOrder(long userId, long orderId) {
        final Optional<OrderRecord> targetOrder = orderQueries.getOrderForUser(userId, orderId);
        if (targetOrder.isEmpty()) {
            return Optional.empty();
        }

        final OrderRecord order = targetOrder.get();
        final double closePrice = marketPriceService.resolveCurrentPrice(order.getSymbol(), order.getPrice());
        final OrderRecord closedOrder = orderCommands.closeOrder(userId, orderId, closePrice).orElse(order);
        return Optional.of(toResponse(closedOrder, CurrencyCode.USD, null));
    }

    private OrderResponseDto toResponse(OrderRecord order, CurrencyCode displayCurrency, Double cachedCurrentPrice) {
        final OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setId(order.getId());
        responseDto.setUserId(order.getUserId());
        responseDto.setSymbol(order.getSymbol());
        responseDto.setSide(order.getSide() == null ? null : order.getSide().name());
        responseDto.setCurrency(displayCurrency.name());
        responseDto.setQuantity(order.getQuantity());
        responseDto.setStatus(order.getStatus().name());
        responseDto.setCreatedAt(order.getCreatedAt());
        responseDto.setClosedAt(order.getClosedAt());
        responseDto.setAiPrediction(order.getAiPrediction());
        responseDto.setBullScore(order.getBullScore());

        final boolean closed = order.getStatus() == OrderStatus.CLOSED && order.getClosePrice() != null;
        final CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(order.getSymbol());
        final double rawCurrentPrice;
        if (closed) {
            rawCurrentPrice = order.getClosePrice();
        } else if (cachedCurrentPrice != null && cachedCurrentPrice > 0.0) {
            rawCurrentPrice = cachedCurrentPrice;
        } else {
            rawCurrentPrice = order.getPrice();
        }

        final double rawEntry = order.getPrice();
        final Instant entryTimestamp = order.getCreatedAt() == null ? Instant.now() : order.getCreatedAt();
        final Instant currentTimestamp = closed && order.getClosedAt() != null ? order.getClosedAt() : Instant.now();
        final double entry = currencyConversionService.convertAmount(
            rawEntry,
            sourceCurrency,
            displayCurrency,
            entryTimestamp
        );
        final double currentPrice = currencyConversionService.convertAmount(
            rawCurrentPrice,
            sourceCurrency,
            displayCurrency,
            currentTimestamp
        );
        final Double closePrice = order.getClosePrice() == null ? null : currencyConversionService.convertAmount(
            order.getClosePrice(),
            sourceCurrency,
            displayCurrency,
            currentTimestamp
        );

        final double rawPnlPerUnit = order.getSide() == OrderSide.BUY
            ? rawCurrentPrice - rawEntry
            : rawEntry - rawCurrentPrice;
        final double pnlPerUnit = currencyConversionService.convertAmount(
            rawPnlPerUnit,
            sourceCurrency,
            displayCurrency,
            currentTimestamp
        );
        final double pnl = pnlPerUnit * order.getQuantity();
        final double pnlPercent = rawEntry == 0 ? 0 : (rawPnlPerUnit / rawEntry) * 100.0;

        responseDto.setPrice(roundCurrency(entry));
        responseDto.setCurrentPrice(roundCurrency(currentPrice));
        responseDto.setPnl(roundCurrency(pnl));
        responseDto.setPnlPercent(roundCurrency(pnlPercent));
        responseDto.setNetValue(roundCurrency(currentPrice * order.getQuantity()));
        responseDto.setTiming(closed ? "CLOSED" : timing(pnlPerUnit));
        responseDto.setClosePrice(closePrice == null ? null : roundCurrency(closePrice));
        return responseDto;
    }

    private void prefetchOrderRates(List<OrderRecord> orders, CurrencyCode displayCurrency) {
        final Map<CurrencyCode, Instant[]> ranges = new EnumMap<>(CurrencyCode.class);
        final Instant now = Instant.now();
        for (OrderRecord order : orders) {
            final CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(order.getSymbol());
            if (sourceCurrency == displayCurrency) {
                continue;
            }
            final Instant start = order.getCreatedAt() == null ? now : order.getCreatedAt();
            final Instant end = order.getClosedAt() == null ? now : order.getClosedAt();
            final Instant[] range = ranges.computeIfAbsent(sourceCurrency, ignored -> new Instant[]{start, end});
            if (start.isBefore(range[0])) {
                range[0] = start;
            }
            if (end.isAfter(range[1])) {
                range[1] = end;
            }
        }
        ranges.forEach((source, range) -> currencyConversionService.prefetchRates(
            source,
            displayCurrency,
            range[0],
            range[1]
        ));
    }

    private String timing(double pnlPerUnit) {
        if (pnlPerUnit > 0) {
            return "GOOD";
        }
        if (pnlPerUnit < 0) {
            return "BAD";
        }
        return "NEUTRAL";
    }
}
