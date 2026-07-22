package com.tradernet.portfolio;

import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.order.OrderPortfolioItem;
import com.tradernet.order.OrderPortfolioQueryService;
import com.tradernet.portfolio.dto.PortfolioSummaryDto;
import com.tradernet.portfolio.position.PortfolioPositionEvent;
import com.tradernet.portfolio.position.PortfolioPositionService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.Instant;
import java.util.List;

/**
 * Orchestrates user-scoped portfolio position, valuation, and history collaborators.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PortfolioService implements PortfolioQueryService {

    @EJB
    private OrderPortfolioQueryService orderPortfolioQueryService;

    @EJB
    private PortfolioPositionService positionService;

    @EJB
    private PortfolioValuationService valuationService;

    @EJB
    private PortfolioHistoryService historyService;

    @Override
    public PortfolioSummaryDto getPortfolio(long userId, String currency) {
        final CurrencyCode displayCurrency = CurrencyCode.parseOrDefault(currency, CurrencyCode.USD);
        final Instant now = Instant.now();
        final List<OrderPortfolioItem> orders = orderPortfolioQueryService.getPortfolioItems(userId);
        final List<PortfolioPositionEvent> events = positionService.buildEvents(orders, now);

        historyService.prefetchConversionRates(events, displayCurrency, now);
        final PortfolioSummaryDto summary = valuationService.buildSummary(events, displayCurrency, now);
        summary.setHistory(historyService.buildHistory(events, displayCurrency, now));
        return summary;
    }
}
