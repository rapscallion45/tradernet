package com.tradernet.order.portfolio;

import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.currencyconversion.CurrencyConversionService;
import com.tradernet.order.MarketPriceService;
import com.tradernet.order.dto.PortfolioAssetDto;
import com.tradernet.order.dto.PortfolioSummaryDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.tradernet.order.CurrencyRounding.roundCurrency;

/**
 * Values current net positions and builds the portfolio summary contract.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PortfolioValuationService {

    @EJB
    private PortfolioPositionService positionService;

    @EJB
    private MarketPriceService marketPriceService;

    @EJB
    private CurrencyConversionService currencyConversionService;

    public PortfolioSummaryDto buildSummary(
        List<PortfolioPositionEvent> events,
        CurrencyCode displayCurrency,
        Instant now
    ) {
        final Map<String, PortfolioPosition> positions = positionService.aggregate(events);
        final List<PortfolioAssetDto> assets = new ArrayList<>();
        double totalCost = 0.0;
        double totalCostBasis = 0.0;
        double totalMarketValue = 0.0;

        for (Map.Entry<String, PortfolioPosition> entry : positions.entrySet()) {
            final PortfolioPosition position = entry.getValue();
            if (position.isFlat()) {
                continue;
            }

            final String symbol = entry.getKey();
            final CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(symbol);
            final double averageCostRaw = position.getAverageCost();
            final double fallbackPrice = position.getLastKnownPrice() > 0.0
                ? position.getLastKnownPrice()
                : averageCostRaw;
            final double currentPriceRaw = marketPriceService.resolveCurrentPrice(symbol, fallbackPrice);
            final double averageCost = currencyConversionService.convertAmount(
                averageCostRaw,
                sourceCurrency,
                displayCurrency,
                now
            );
            final double currentPrice = currencyConversionService.convertAmount(
                currentPriceRaw,
                sourceCurrency,
                displayCurrency,
                now
            );
            final double assetCost = averageCost * position.getNetQuantity();
            final double marketValue = currentPrice * position.getNetQuantity();
            final double pnl = marketValue - assetCost;
            final double costBasis = Math.abs(assetCost);

            assets.add(asset(symbol, position, averageCost, currentPrice, assetCost, marketValue, pnl, costBasis));
            totalCost += assetCost;
            totalCostBasis += costBasis;
            totalMarketValue += marketValue;
        }

        assets.sort(Comparator.comparingDouble((PortfolioAssetDto asset) -> Math.abs(asset.getMarketValue())).reversed());
        return summary(displayCurrency, assets, totalCost, totalCostBasis, totalMarketValue);
    }

    private PortfolioAssetDto asset(
        String symbol,
        PortfolioPosition position,
        double averageCost,
        double currentPrice,
        double assetCost,
        double marketValue,
        double pnl,
        double costBasis
    ) {
        final PortfolioAssetDto asset = new PortfolioAssetDto();
        asset.setSymbol(symbol);
        asset.setQuantity(position.getNetQuantity());
        asset.setAverageCost(roundCurrency(averageCost));
        asset.setCurrentPrice(roundCurrency(currentPrice));
        asset.setTotalCost(roundCurrency(assetCost));
        asset.setMarketValue(roundCurrency(marketValue));
        asset.setProfitLoss(roundCurrency(pnl));
        asset.setProfitLossPercent(roundCurrency(costBasis == 0.0 ? 0.0 : (pnl / costBasis) * 100.0));
        return asset;
    }

    private PortfolioSummaryDto summary(
        CurrencyCode displayCurrency,
        List<PortfolioAssetDto> assets,
        double totalCost,
        double totalCostBasis,
        double totalMarketValue
    ) {
        final PortfolioSummaryDto summary = new PortfolioSummaryDto();
        summary.setCurrency(displayCurrency.name());
        summary.setAssets(assets);
        summary.setTotalCost(roundCurrency(totalCost));
        summary.setTotalMarketValue(roundCurrency(totalMarketValue));
        final double totalPnl = totalMarketValue - totalCost;
        summary.setTotalProfitLoss(roundCurrency(totalPnl));
        summary.setTotalProfitLossPercent(roundCurrency(
            totalCostBasis == 0.0 ? 0.0 : (totalPnl / totalCostBasis) * 100.0
        ));
        return summary;
    }
}
