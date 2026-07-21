package com.tradernet.order;

import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.order.dto.PortfolioSummaryDto;
import com.tradernet.order.portfolio.PortfolioHistoryService;
import com.tradernet.order.portfolio.PortfolioPositionEvent;
import com.tradernet.order.portfolio.PortfolioPositionService;
import com.tradernet.order.portfolio.PortfolioValuationService;
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
public class PortfolioService {

    @EJB
    private OrderService orderService;

    @EJB
    private PortfolioPositionService positionService;

    @EJB
    private PortfolioValuationService valuationService;

    @EJB
    private PortfolioHistoryService historyService;

    public PortfolioSummaryDto getPortfolio(long userId, String currency) {
        final CurrencyCode displayCurrency = CurrencyCode.parseOrDefault(currency, CurrencyCode.USD);
        final Instant now = Instant.now();
        final List<OrderEntity> orders = orderService.getOrdersByUserId(userId);
        final List<PortfolioPositionEvent> events = positionService.buildEvents(orders, now);

        historyService.prefetchConversionRates(events, displayCurrency, now);
        final PortfolioSummaryDto summary = valuationService.buildSummary(events, displayCurrency, now);
        summary.setHistory(historyService.buildHistory(events, displayCurrency, now));
        return summary;
    }
}
