package com.tradernet.order.portfolio;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.order.OrderService;
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
 * Maps persisted orders into signed events and replays them into net positions.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PortfolioPositionService {

    public List<PortfolioPositionEvent> buildEvents(List<OrderEntity> orders, Instant fallbackTime) {
        final List<PortfolioPositionEvent> events = new ArrayList<>();
        for (OrderEntity order : orders) {
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

    private void appendOrderEvents(List<PortfolioPositionEvent> events, OrderEntity order, Instant fallbackTime) {
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

        final boolean closed = OrderService.CLOSED_STATUS.equals(order.getStatus()) && order.getClosePrice() != null;
        if (!closed) {
            return;
        }

        final PortfolioPositionSide closeSide = openSide == PortfolioPositionSide.BUY
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
