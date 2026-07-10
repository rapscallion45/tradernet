package com.tradernet.trade;

import com.tradernet.jpa.dao.TradeDao;
import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.jpa.entities.TradeEntity;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Locale;

/**
 * Executes orders and persists the resulting trade fill.
 * In a real system this would integrate with a broker/exchange.
 */
@Stateless
public class TradeExecutionService {

    public static final String OPEN_EXECUTION_TYPE = "OPEN";
    public static final String CLOSE_EXECUTION_TYPE = "CLOSE";

    @Inject
    private TradeDao tradeDao;

    /**
     * Executes the given order.
     *
     * @param order order to execute
     * @return Trade object representing the completed trade
     */
    public TradeEntity execute(OrderEntity order) {
        requireOrder(order);
        return execute(order, order.getSide(), OPEN_EXECUTION_TYPE, order.getPrice());
    }

    /**
     * Executes the inverse trade required to close an order.
     *
     * @param order open order being closed
     * @param closePrice close execution price
     * @return Trade object representing the completed close fill
     */
    public TradeEntity executeClose(OrderEntity order, double closePrice) {
        requireOrder(order);
        if (order.getId() != null && tradeDao.existsByOrderIdAndExecutionType(order.getId(), CLOSE_EXECUTION_TYPE)) {
            return null;
        }

        OrderEntity.Side closeSide = order.getSide() == OrderEntity.Side.BUY
            ? OrderEntity.Side.SELL
            : OrderEntity.Side.BUY;
        return execute(order, closeSide, CLOSE_EXECUTION_TYPE, closePrice);
    }

    public List<TradeEntity> getTradesForUser(long userId, String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return tradeDao.findByUserId(userId);
        }
        return tradeDao.findByUserIdAndSymbol(userId, normalizeSymbol(symbol));
    }

    private TradeEntity execute(OrderEntity order, OrderEntity.Side executionSide, String executionType, double executionPrice) {
        requireOrder(order);
        if (executionPrice <= 0) {
            throw new IllegalArgumentException("execution price must be greater than 0");
        }

        TradeEntity trade = new TradeEntity(
            order.getUserId(),
            order.getId(),
            normalizeSymbol(order.getSymbol()),
            executionSide.name(),
            executionType,
            signedQuantity(executionSide, order.getQuantity()),
            executionPrice
        );
        tradeDao.save(trade);
        return trade;
    }

    private void requireOrder(OrderEntity order) {
        if (order == null) {
            throw new IllegalArgumentException("order is required");
        }
        if (order.getSymbol() == null || order.getSymbol().isBlank()) {
            throw new IllegalArgumentException("order symbol is required");
        }
        if (order.getSide() == null) {
            throw new IllegalArgumentException("order side is required");
        }
        if (order.getUserId() == null) {
            throw new IllegalArgumentException("order user id is required");
        }
        if (order.getQuantity() <= 0) {
            throw new IllegalArgumentException("order quantity must be greater than 0");
        }
    }

    private double signedQuantity(OrderEntity.Side side, double quantity) {
        return side == OrderEntity.Side.SELL ? -quantity : quantity;
    }

    private String normalizeSymbol(String symbol) {
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
