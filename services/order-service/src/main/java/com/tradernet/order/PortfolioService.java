package com.tradernet.order;

import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.currencyconversion.CurrencyConversionService;
import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.order.dto.PortfolioAssetDto;
import com.tradernet.order.dto.PortfolioHistoryEventDto;
import com.tradernet.order.dto.PortfolioHistoryPointDto;
import com.tradernet.order.dto.PortfolioSummaryDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * Builds user-scoped portfolio holdings, valuation, and history from order and market data.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PortfolioService {

    private static final int MAX_PORTFOLIO_HISTORY_DAYS = 1_000;

    @EJB
    private OrderService orderService;

    @EJB
    private MarketAiService marketAiService;

    @EJB
    private CurrencyConversionService currencyConversionService;

    public PortfolioSummaryDto getPortfolio(long userId, String currency) {
        final CurrencyCode displayCurrency = CurrencyCode.parseOrDefault(currency, CurrencyCode.USD);
        final Instant now = Instant.now();

        final List<OrderEntity> orders = orderService.getOrdersByUserId(userId);
        final List<PositionEvent> positionEvents = buildPositionEvents(orders, now);

        final PortfolioSummaryDto summary = buildPortfolioSummary(positionEvents, displayCurrency, now);
        summary.setHistory(buildHistoryFromEvents(positionEvents, displayCurrency, now));
        return summary;
    }

    private PortfolioSummaryDto buildPortfolioSummary(List<PositionEvent> positionEvents, CurrencyCode displayCurrency, Instant now) {
        final Map<String, PositionAggregate> positions = new HashMap<>();

        for (PositionEvent event : positionEvents) {
            final PositionAggregate aggregate = positions.computeIfAbsent(event.symbol, ignored -> new PositionAggregate());
            applyTrade(aggregate, event.quantityDelta, event.price);
            aggregate.lastKnownPrice = event.price;
        }

        final List<PortfolioAssetDto> assets = new ArrayList<>();
        double totalCost = 0.0;
        double totalMarketValue = 0.0;

        for (Map.Entry<String, PositionAggregate> entry : positions.entrySet()) {
            final PositionAggregate aggregate = entry.getValue();
            if (aggregate.netQuantity <= 0.0) {
                continue;
            }

            final String symbol = entry.getKey();
            final CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(symbol);

            final double averageCostRaw = aggregate.netCost <= 0.0 ? 0.0 : aggregate.netCost / aggregate.netQuantity;
            final double fallbackPrice = aggregate.lastKnownPrice > 0.0 ? aggregate.lastKnownPrice : averageCostRaw;
            final double currentPriceRaw = resolveCurrentPrice(symbol, fallbackPrice);

            final double averageCost = currencyConversionService.convertAmount(averageCostRaw, sourceCurrency, displayCurrency, now);
            final double currentPrice = currencyConversionService.convertAmount(currentPriceRaw, sourceCurrency, displayCurrency, now);
            final double assetCost = averageCost * aggregate.netQuantity;
            final double marketValue = currentPrice * aggregate.netQuantity;
            final double pnl = marketValue - assetCost;
            final double pnlPercent = assetCost == 0.0 ? 0.0 : (pnl / assetCost) * 100.0;

            final PortfolioAssetDto asset = new PortfolioAssetDto();
            asset.setSymbol(symbol);
            asset.setQuantity(aggregate.netQuantity);
            asset.setAverageCost(roundCurrency(averageCost));
            asset.setCurrentPrice(roundCurrency(currentPrice));
            asset.setTotalCost(roundCurrency(assetCost));
            asset.setMarketValue(roundCurrency(marketValue));
            asset.setProfitLoss(roundCurrency(pnl));
            asset.setProfitLossPercent(roundCurrency(pnlPercent));

            assets.add(asset);
            totalCost += assetCost;
            totalMarketValue += marketValue;
        }

        assets.sort(Comparator.comparingDouble(PortfolioAssetDto::getMarketValue).reversed());

        final PortfolioSummaryDto summary = new PortfolioSummaryDto();
        summary.setCurrency(displayCurrency.name());
        summary.setAssets(assets);
        summary.setTotalCost(roundCurrency(totalCost));
        summary.setTotalMarketValue(roundCurrency(totalMarketValue));

        final double totalPnl = totalMarketValue - totalCost;
        summary.setTotalProfitLoss(roundCurrency(totalPnl));
        summary.setTotalProfitLossPercent(roundCurrency(totalCost == 0.0 ? 0.0 : (totalPnl / totalCost) * 100.0));
        return summary;
    }

    private List<PortfolioHistoryPointDto> buildHistoryFromEvents(List<PositionEvent> positionEvents, CurrencyCode displayCurrency, Instant now) {
        final List<PortfolioHistoryPointDto> history = new ArrayList<>();
        if (positionEvents.isEmpty()) {
            return history;
        }

        final Map<String, PositionAggregate> rollingPositions = new HashMap<>();
        final Map<LocalDate, List<PositionEvent>> eventsByDate = new HashMap<>();
        final Set<String> symbols = new HashSet<>();

        LocalDate firstDate = toUtcDate(positionEvents.get(0).timestamp);
        final LocalDate today = toUtcDate(now);
        if (firstDate.isAfter(today)) {
            firstDate = today;
        }

        final long requestedDays = ChronoUnit.DAYS.between(firstDate, today) + 1L;
        final int historyDays = (int) Math.max(1L, Math.min(requestedDays, MAX_PORTFOLIO_HISTORY_DAYS));
        final LocalDate startDate = today.minusDays(historyDays - 1L);

        for (PositionEvent event : positionEvents) {
            final LocalDate eventDate = toUtcDate(event.timestamp);
            symbols.add(event.symbol);
            eventsByDate.computeIfAbsent(eventDate, ignored -> new ArrayList<>()).add(event);
            if (eventDate.isBefore(startDate)) {
                applyPositionEvent(rollingPositions, event);
            }
        }

        final Map<String, NavigableMap<LocalDate, Double>> dailyClosePrices = loadDailyClosePrices(symbols, historyDays + 5);

        for (int offset = 0; offset < historyDays; offset += 1) {
            final LocalDate date = startDate.plusDays(offset);
            final List<PositionEvent> eventsForDate = eventsByDate.getOrDefault(date, List.of());
            for (PositionEvent event : eventsForDate) {
                applyPositionEvent(rollingPositions, event);
            }

            final boolean isToday = date.equals(today);
            final Instant valuationTime = isToday ? now : date.atStartOfDay(ZoneOffset.UTC).toInstant();
            final double accountValue = calculateAccountValue(rollingPositions, displayCurrency, valuationTime, date, isToday, dailyClosePrices);
            history.add(new PortfolioHistoryPointDto(
                valuationTime.toEpochMilli(),
                roundCurrency(accountValue),
                buildHistoryEventDtos(eventsForDate, displayCurrency)
            ));
        }

        return history;
    }

    private double calculateAccountValue(
        Map<String, PositionAggregate> positions,
        CurrencyCode displayCurrency,
        Instant timestamp,
        LocalDate valuationDate,
        boolean useLivePrice,
        Map<String, NavigableMap<LocalDate, Double>> dailyClosePrices
    ) {
        double totalValue = 0.0;

        for (Map.Entry<String, PositionAggregate> entry : positions.entrySet()) {
            final PositionAggregate aggregate = entry.getValue();
            if (aggregate.netQuantity <= 0.0) {
                continue;
            }

            final String symbol = entry.getKey();
            final double fallbackPrice = aggregate.lastKnownPrice > 0.0 ? aggregate.lastKnownPrice : aggregate.netCost / aggregate.netQuantity;
            final double priceRaw = useLivePrice
                ? resolveCurrentPrice(symbol, fallbackPrice)
                : resolveHistoricalPrice(symbol, valuationDate, fallbackPrice, dailyClosePrices);

            final CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(symbol);
            final double convertedPrice = currencyConversionService.convertAmount(priceRaw, sourceCurrency, displayCurrency, timestamp);
            totalValue += aggregate.netQuantity * convertedPrice;
        }

        return totalValue;
    }

    private List<PositionEvent> buildPositionEvents(List<OrderEntity> orders, Instant fallbackTime) {
        final List<PositionEvent> events = new ArrayList<>();

        for (OrderEntity order : orders) {
            if (order == null || order.getSymbol() == null || order.getSymbol().isBlank()) {
                continue;
            }

            final String symbol = order.getSymbol().trim().toUpperCase(Locale.ROOT);
            final OrderEntity.Side orderSide = order.getSide();
            if (orderSide == null) {
                continue;
            }

            final double signedQuantity = orderSide == OrderEntity.Side.BUY ? order.getQuantity() : -order.getQuantity();
            final Instant openTimestamp = order.getCreatedAt() != null ? order.getCreatedAt() : fallbackTime;

            events.add(new PositionEvent(symbol, orderSide, signedQuantity, order.getPrice(), openTimestamp));

            final boolean isClosed = OrderService.CLOSED_STATUS.equals(order.getStatus()) && order.getClosePrice() != null;
            if (isClosed) {
                final Instant closeTimestamp = order.getClosedAt() != null ? order.getClosedAt() : openTimestamp;
                final OrderEntity.Side closeSide = orderSide == OrderEntity.Side.BUY ? OrderEntity.Side.SELL : OrderEntity.Side.BUY;
                events.add(new PositionEvent(symbol, closeSide, -signedQuantity, order.getClosePrice(), closeTimestamp));
            }
        }

        events.sort(Comparator.comparing(event -> event.timestamp));
        return events;
    }

    private void applyPositionEvent(Map<String, PositionAggregate> positions, PositionEvent event) {
        final PositionAggregate aggregate = positions.computeIfAbsent(event.symbol, ignored -> new PositionAggregate());
        applyTrade(aggregate, event.quantityDelta, event.price);
        aggregate.lastKnownPrice = event.price;
    }

    private List<PortfolioHistoryEventDto> buildHistoryEventDtos(List<PositionEvent> events, CurrencyCode displayCurrency) {
        final List<PortfolioHistoryEventDto> eventDtos = new ArrayList<>();
        for (PositionEvent event : events) {
            final CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(event.symbol);
            final double convertedPrice = currencyConversionService.convertAmount(event.price, sourceCurrency, displayCurrency, event.timestamp);
            eventDtos.add(new PortfolioHistoryEventDto(
                event.symbol,
                event.side.name(),
                Math.abs(event.quantityDelta),
                roundCurrency(convertedPrice),
                event.timestamp.toEpochMilli()
            ));
        }
        return eventDtos;
    }

    private Map<String, NavigableMap<LocalDate, Double>> loadDailyClosePrices(Set<String> symbols, int requestedDays) {
        final Map<String, NavigableMap<LocalDate, Double>> pricesBySymbol = new HashMap<>();
        final int limit = Math.max(1, Math.min(requestedDays, MAX_PORTFOLIO_HISTORY_DAYS));

        for (String symbol : symbols) {
            final NavigableMap<LocalDate, Double> closesByDate = new TreeMap<>();
            final List<MarketBar> dailyBars = marketAiService.getBars(symbol, "1D", limit);
            for (MarketBar bar : dailyBars) {
                if (bar == null || bar.getClose() <= 0.0) {
                    continue;
                }
                closesByDate.put(toUtcDate(Instant.ofEpochMilli(bar.getBucketStart())), bar.getClose());
            }
            pricesBySymbol.put(symbol, closesByDate);
        }

        return pricesBySymbol;
    }

    private double resolveHistoricalPrice(
        String symbol,
        LocalDate valuationDate,
        double fallbackPrice,
        Map<String, NavigableMap<LocalDate, Double>> dailyClosePrices
    ) {
        final NavigableMap<LocalDate, Double> closesByDate = dailyClosePrices.get(symbol);
        if (closesByDate == null || closesByDate.isEmpty()) {
            return fallbackPrice;
        }

        final Map.Entry<LocalDate, Double> close = closesByDate.floorEntry(valuationDate);
        if (close == null || close.getValue() == null || close.getValue() <= 0.0) {
            return fallbackPrice;
        }

        return close.getValue();
    }

    private LocalDate toUtcDate(Instant instant) {
        return LocalDate.ofInstant(instant, ZoneOffset.UTC);
    }

    private void applyTrade(PositionAggregate aggregate, double quantityDelta, double tradePrice) {
        if (Math.abs(quantityDelta) < 1e-9) {
            return;
        }

        if (Math.abs(aggregate.netQuantity) < 1e-9 || hasSameSign(aggregate.netQuantity, quantityDelta)) {
            aggregate.netQuantity += quantityDelta;
            aggregate.netCost += quantityDelta * tradePrice;
            if (Math.abs(aggregate.netQuantity) < 1e-9) {
                aggregate.netQuantity = 0.0;
                aggregate.netCost = 0.0;
            }
            return;
        }

        final double newQuantity = aggregate.netQuantity + quantityDelta;
        final double averageCost = Math.abs(aggregate.netCost / aggregate.netQuantity);

        if (Math.abs(newQuantity) < 1e-9) {
            aggregate.netQuantity = 0.0;
            aggregate.netCost = 0.0;
            return;
        }

        if (hasSameSign(aggregate.netQuantity, newQuantity)) {
            aggregate.netQuantity = newQuantity;
            aggregate.netCost = Math.copySign(Math.abs(newQuantity) * averageCost, newQuantity);
            return;
        }

        aggregate.netQuantity = newQuantity;
        aggregate.netCost = newQuantity * tradePrice;
    }

    private double roundCurrency(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private boolean hasSameSign(double left, double right) {
        return (left > 0.0 && right > 0.0) || (left < 0.0 && right < 0.0);
    }

    private double resolveCurrentPrice(String symbol, double fallbackPrice) {
        final List<MarketBar> bars = marketAiService.getBars(symbol, "1S", 1);
        if (bars == null || bars.isEmpty() || bars.get(0) == null || bars.get(0).getClose() <= 0.0) {
            return fallbackPrice;
        }
        return bars.get(0).getClose();
    }

    private static class PositionAggregate {
        private double netQuantity;
        private double netCost;
        private double lastKnownPrice;
    }

    private static class PositionEvent {
        private final String symbol;
        private final OrderEntity.Side side;
        private final double quantityDelta;
        private final double price;
        private final Instant timestamp;

        private PositionEvent(String symbol, OrderEntity.Side side, double quantityDelta, double price, Instant timestamp) {
            this.symbol = symbol;
            this.side = side;
            this.quantityDelta = quantityDelta;
            this.price = price;
            this.timestamp = timestamp;
        }
    }
}
