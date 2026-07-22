package com.tradernet.portfolio.position;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.order.OrderPortfolioItem;
import com.tradernet.order.dto.OrderSide;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps order projections into signed events and replays them into net positions.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PortfolioPositionService {

    public List<PortfolioPositionEvent> buildEvents(List<OrderPortfolioItem> orders, Instant fallbackTime) {
        final List<PortfolioPositionEvent> events = new ArrayList<>();
        for (OrderPortfolioItem order : orders) {
            appendOrderEvents(events, order, fallbackTime);
        }
        events.sort(Comparator.comparing(PortfolioPositionEvent::getTimestamp));
        return events;
    }

    public Map<String, PortfolioPosition> aggregate(List<PortfolioPositionEvent> events) {
        final Map<String, PortfolioPosition> positions = new HashMap<>();
        for (PortfolioPositionEvent event : events) {
            applyEvent(positions, event);
        }
        return positions;
    }

    public void applyEvent(Map<String, PortfolioPosition> positions, PortfolioPositionEvent event) {
        final PortfolioPosition position = positions.computeIfAbsent(
            event.getSymbol(),
            ignored -> new PortfolioPosition()
        );
        position.apply(event.getQuantityDelta(), event.getPrice());
        position.setLastKnownPrice(event.getPrice());
    }

    private void appendOrderEvents(
        List<PortfolioPositionEvent> events,
        OrderPortfolioItem order,
        Instant fallbackTime
    ) {
        if (order == null || order.getSymbol() == null || order.getSymbol().isBlank() || order.getSide() == null) {
            return;
        }

        final String symbol = MarketSymbolNormalizer.normalizeSymbol(order.getSymbol());
        final PortfolioPositionSide openSide = PortfolioPositionSide.valueOf(order.getSide().name());
        final double signedQuantity = openSide == PortfolioPositionSide.BUY
            ? order.getQuantity()
            : -order.getQuantity();
        final Instant openTimestamp = order.getCreatedAt() == null ? fallbackTime : order.getCreatedAt();
        events.add(new PortfolioPositionEvent(symbol, openSide, signedQuantity, order.getPrice(), openTimestamp));

        if (!order.isClosed() || order.getClosePrice() == null) {
            return;
        }

        final PortfolioPositionSide closeSide = order.getSide() == OrderSide.BUY
            ? PortfolioPositionSide.SELL
            : PortfolioPositionSide.BUY;
        final Instant closeTimestamp = order.getClosedAt() == null ? openTimestamp : order.getClosedAt();
        events.add(new PortfolioPositionEvent(
            symbol,
            closeSide,
            -signedQuantity,
            order.getClosePrice(),
            closeTimestamp
        ));
    }
}
