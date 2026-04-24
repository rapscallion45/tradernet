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
import java.time.temporal.ChronoUnit;
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
        PortfolioSummaryDto summary = buildPortfolioSummary(orders, displayCurrency, now);

        if ("standard".equalsIgnoreCase(authUser.get().getUsername())) {
            summary.setHistory(buildSampleHistory(summary.getTotalMarketValue(), now));
        } else {
            summary.setHistory(buildSimpleHistory(summary.getTotalMarketValue(), now));
        }

        return Response.ok(summary).build();
    }

    private PortfolioSummaryDto buildPortfolioSummary(List<OrderEntity> orders, CurrencyCode displayCurrency, Instant now) {
        Map<String, PositionAggregate> positions = new HashMap<>();

        for (OrderEntity order : orders) {
            if (order == null || order.getSymbol() == null || order.getSymbol().isBlank()) {
                continue;
            }

            String symbol = order.getSymbol().trim().toUpperCase(Locale.ROOT);
            PositionAggregate aggregate = positions.computeIfAbsent(symbol, ignored -> new PositionAggregate());

            double quantity = order.getQuantity();
            double signedQuantity = order.getSide() == OrderEntity.Side.BUY ? quantity : -quantity;
            double signedCost = signedQuantity * order.getPrice();

            aggregate.netQuantity += signedQuantity;
            aggregate.netCost += signedCost;
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
            double currentPriceRaw = resolveCurrentPrice(symbol, averageCostRaw > 0 ? averageCostRaw : 1);

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

    private List<PortfolioHistoryPointDto> buildSampleHistory(double currentValue, Instant now) {
        List<PortfolioHistoryPointDto> history = new ArrayList<>();
        double safeCurrentValue = currentValue <= 0 ? 10000 : currentValue;
        Instant start = now.minus(24, ChronoUnit.MONTHS);

        for (int i = 0; i <= 24; i++) {
            double growth = (double) i / 24.0;
            double seasonal = Math.sin(i * 0.9) * 0.06;
            double value = safeCurrentValue * (0.35 + (growth * 0.65) + seasonal);
            history.add(new PortfolioHistoryPointDto(start.plus(i, ChronoUnit.MONTHS).toEpochMilli(), Math.max(250, value)));
        }

        return history;
    }

    private List<PortfolioHistoryPointDto> buildSimpleHistory(double currentValue, Instant now) {
        List<PortfolioHistoryPointDto> history = new ArrayList<>();
        double safeCurrentValue = currentValue <= 0 ? 1000 : currentValue;

        history.add(new PortfolioHistoryPointDto(now.minus(30, ChronoUnit.DAYS).toEpochMilli(), safeCurrentValue * 0.92));
        history.add(new PortfolioHistoryPointDto(now.minus(14, ChronoUnit.DAYS).toEpochMilli(), safeCurrentValue * 0.96));
        history.add(new PortfolioHistoryPointDto(now.toEpochMilli(), safeCurrentValue));
        return history;
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
    }
}
