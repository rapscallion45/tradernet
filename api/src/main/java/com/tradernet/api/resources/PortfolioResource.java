package com.tradernet.api.resources;

import com.tradernet.api.resources.dto.PortfolioAssetDto;
import com.tradernet.api.resources.dto.PortfolioHistoryPointDto;
import com.tradernet.api.resources.dto.PortfolioSummaryDto;
import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.currencyconversion.CurrencyConversionService;
import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.order.OrderService;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for viewing portfolio holdings and performance.
 */
@Path("/portfolio")
@Produces(MediaType.APPLICATION_JSON)
public class PortfolioResource {

    @Inject
    private OrderService orderService;

    @Inject
    private MarketAiService marketAiService;

    @Inject
    private CurrencyConversionService currencyConversionService;

    @GET
    public Response getPortfolio(
        @CookieParam(AuthResource.SESSION_COOKIE_NAME) String sessionId,
        @DefaultValue("USD") @QueryParam("currency") String currency
    ) {
        Optional<AuthUserDto> authUser = AuthResource.getSessionUser(sessionId);
        if (authUser.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("Not authenticated")
                .build();
        }

        CurrencyCode displayCurrency = CurrencyCode.parseOrDefault(currency, CurrencyCode.USD);
        Instant now = Instant.now();

        List<OrderEntity> orders = orderService.getOrdersByUserId(authUser.get().getId());
        List<PositionEvent> positionEvents = buildPositionEvents(orders, now);

        PortfolioSummaryDto summary = buildPortfolioSummary(positionEvents, displayCurrency, now);
        summary.setHistory(buildHistoryFromEvents(positionEvents, displayCurrency, now));

        return Response.ok(summary).build();
    }

    private PortfolioSummaryDto buildPortfolioSummary(List<PositionEvent> positionEvents, CurrencyCode displayCurrency, Instant now) {
        Map<String, PositionAggregate> positions = new HashMap<>();

        for (PositionEvent event : positionEvents) {
            PositionAggregate aggregate = positions.computeIfAbsent(event.symbol, ignored -> new PositionAggregate());
            applyTrade(aggregate, event.quantityDelta, event.price);
            aggregate.lastKnownPrice = event.price;
        }

        List<PortfolioAssetDto> assets = new ArrayList<>();
        double totalCost = 0;
        double totalMarketValue = 0;

        for (Map.Entry<String, PositionAggregate> entry : positions.entrySet()) {
            PositionAggregate aggregate = entry.getValue();
            if (aggregate.netQuantity <= 0) {
                continue;
            }

            String symbol = entry.getKey();
            CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(symbol);

            double averageCostRaw = aggregate.netCost <= 0 ? 0 : aggregate.netCost / aggregate.netQuantity;
            double currentPriceRaw = resolveCurrentPrice(symbol, aggregate.lastKnownPrice > 0 ? aggregate.lastKnownPrice : averageCostRaw);

            double averageCost = currencyConversionService.convertAmount(averageCostRaw, sourceCurrency, displayCurrency, now);
            double currentPrice = currencyConversionService.convertAmount(currentPriceRaw, sourceCurrency, displayCurrency, now);
            double assetCost = averageCost * aggregate.netQuantity;
            double marketValue = currentPrice * aggregate.netQuantity;
            double pnl = marketValue - assetCost;
            double pnlPercent = assetCost == 0 ? 0 : (pnl / assetCost) * 100.0;

            PortfolioAssetDto asset = new PortfolioAssetDto();
            asset.setSymbol(symbol);
            asset.setQuantity(aggregate.netQuantity);
            asset.setAverageCost(averageCost);
            asset.setCurrentPrice(currentPrice);
            asset.setTotalCost(assetCost);
            asset.setMarketValue(marketValue);
            asset.setProfitLoss(pnl);
            asset.setProfitLossPercent(pnlPercent);

            assets.add(asset);
            totalCost += assetCost;
            totalMarketValue += marketValue;
        }

        assets.sort(Comparator.comparingDouble(PortfolioAssetDto::getMarketValue).reversed());

        PortfolioSummaryDto summary = new PortfolioSummaryDto();
        summary.setCurrency(displayCurrency.name());
        summary.setAssets(assets);
        summary.setTotalCost(totalCost);
        summary.setTotalMarketValue(totalMarketValue);

        double totalPnl = totalMarketValue - totalCost;
        summary.setTotalProfitLoss(totalPnl);
        summary.setTotalProfitLossPercent(totalCost == 0 ? 0 : (totalPnl / totalCost) * 100.0);
        return summary;
    }

    private List<PortfolioHistoryPointDto> buildHistoryFromEvents(List<PositionEvent> positionEvents, CurrencyCode displayCurrency, Instant now) {
        List<PortfolioHistoryPointDto> history = new ArrayList<>();
        if (positionEvents.isEmpty()) {
            return history;
        }

        Map<String, PositionAggregate> rollingPositions = new HashMap<>();

        for (PositionEvent event : positionEvents) {
            PositionAggregate aggregate = rollingPositions.computeIfAbsent(event.symbol, ignored -> new PositionAggregate());
            applyTrade(aggregate, event.quantityDelta, event.price);
            aggregate.lastKnownPrice = event.price;

            double accountValue = calculateAccountValue(rollingPositions, displayCurrency, event.timestamp, false);
            history.add(new PortfolioHistoryPointDto(event.timestamp.toEpochMilli(), accountValue));
        }

        double currentAccountValue = calculateAccountValue(rollingPositions, displayCurrency, now, true);
        history.add(new PortfolioHistoryPointDto(now.toEpochMilli(), currentAccountValue));
        return history;
    }

    private double calculateAccountValue(Map<String, PositionAggregate> positions, CurrencyCode displayCurrency, Instant timestamp, boolean useLivePrice) {
        double totalValue = 0;

        for (Map.Entry<String, PositionAggregate> entry : positions.entrySet()) {
            PositionAggregate aggregate = entry.getValue();
            if (aggregate.netQuantity <= 0) {
                continue;
            }

            String symbol = entry.getKey();
            double fallbackPrice = aggregate.lastKnownPrice > 0 ? aggregate.lastKnownPrice : (aggregate.netCost / aggregate.netQuantity);
            double priceRaw = useLivePrice ? resolveCurrentPrice(symbol, fallbackPrice) : fallbackPrice;

            CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(symbol);
            double convertedPrice = currencyConversionService.convertAmount(priceRaw, sourceCurrency, displayCurrency, timestamp);
            totalValue += aggregate.netQuantity * convertedPrice;
        }

        return totalValue;
    }

    private List<PositionEvent> buildPositionEvents(List<OrderEntity> orders, Instant fallbackTime) {
        List<PositionEvent> events = new ArrayList<>();

        for (OrderEntity order : orders) {
            if (order == null || order.getSymbol() == null || order.getSymbol().isBlank()) {
                continue;
            }

            String symbol = order.getSymbol().trim().toUpperCase(Locale.ROOT);
            double signedQuantity = order.getSide() == OrderEntity.Side.BUY ? order.getQuantity() : -order.getQuantity();
            Instant openTimestamp = order.getCreatedAt() != null ? order.getCreatedAt() : fallbackTime;

            events.add(new PositionEvent(symbol, signedQuantity, order.getPrice(), openTimestamp));

            boolean isClosed = OrderService.CLOSED_STATUS.equals(order.getStatus()) && order.getClosePrice() != null;
            if (isClosed) {
                Instant closeTimestamp = order.getClosedAt() != null ? order.getClosedAt() : openTimestamp;
                events.add(new PositionEvent(symbol, -signedQuantity, order.getClosePrice(), closeTimestamp));
            }
        }

        events.sort(Comparator.comparing(event -> event.timestamp));
        return events;
    }

    private void applyTrade(PositionAggregate aggregate, double quantityDelta, double tradePrice) {
        if (Math.abs(quantityDelta) < 1e-9) {
            return;
        }

        if (Math.abs(aggregate.netQuantity) < 1e-9 || hasSameSign(aggregate.netQuantity, quantityDelta)) {
            aggregate.netQuantity += quantityDelta;
            aggregate.netCost += quantityDelta * tradePrice;
            if (Math.abs(aggregate.netQuantity) < 1e-9) {
                aggregate.netQuantity = 0;
                aggregate.netCost = 0;
            }
            return;
        }

        double newQuantity = aggregate.netQuantity + quantityDelta;
        double averageCost = Math.abs(aggregate.netCost / aggregate.netQuantity);

        if (Math.abs(newQuantity) < 1e-9) {
            aggregate.netQuantity = 0;
            aggregate.netCost = 0;
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

    private boolean hasSameSign(double left, double right) {
        return (left > 0 && right > 0) || (left < 0 && right < 0);
    }

    private double resolveCurrentPrice(String symbol, double fallbackPrice) {
        List<MarketBar> bars = marketAiService.getBars(symbol, "1S", 1);
        if (bars == null || bars.isEmpty() || bars.get(0) == null || bars.get(0).getClose() <= 0) {
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
        private final double quantityDelta;
        private final double price;
        private final Instant timestamp;

        private PositionEvent(String symbol, double quantityDelta, double price, Instant timestamp) {
            this.symbol = symbol;
            this.quantityDelta = quantityDelta;
            this.price = price;
            this.timestamp = timestamp;
        }
    }
}
