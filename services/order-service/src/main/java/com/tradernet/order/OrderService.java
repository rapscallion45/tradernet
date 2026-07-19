package com.tradernet.order;

import com.tradernet.jpa.dao.OrderDao;
import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.trade.TradeExecutionRequest;
import com.tradernet.trade.TradeExecutionService;
import com.tradernet.trade.TradeExecutionType;
import com.tradernet.trade.TradeSide;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persists orders, records trade fills, and exposes user-level order retrieval.
 */
@Stateless
public class OrderService {

    public static final String OPEN_STATUS = "OPEN";
    public static final String CLOSED_STATUS = "CLOSED";

    @EJB
    private OrderDao orderDao;

    @EJB
    private TradeExecutionService tradeExecutionService;

    /**
     * Creates and persists an order linked to a user, then records the opening fill.
     *
     * @param userId authenticated user id
     * @param order incoming order payload
     * @return persisted order
     */
    public OrderEntity createOrder(long userId, OrderEntity order) {
        order.setUserId(userId);
        order.setStatus(OPEN_STATUS);
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(Instant.now());
        }
        orderDao.save(order);
        tradeExecutionService.execute(tradeExecutionRequest(order, order.getSide(), TradeExecutionType.OPEN, order.getPrice()));
        return order;
    }

    /**
     * Returns all orders for a specific user sorted by creation time descending.
     */
    public List<OrderEntity> getOrdersByUserId(long userId) {
        return orderDao.findByUserId(userId);
    }

    /**
     * Closes an open order for a user.
     *
     * @param userId authenticated user id
     * @param orderId order id
     * @param closePrice close execution price
     * @return updated order when successful
     */
    public Optional<OrderEntity> closeOrder(long userId, long orderId, double closePrice) {
        Optional<OrderEntity> foundOrder = orderDao.findByIdForUpdate(orderId)
            .filter(order -> order.getUserId() != null && order.getUserId() == userId);

        if (foundOrder.isEmpty()) {
            return Optional.empty();
        }

        OrderEntity order = foundOrder.get();
        if (CLOSED_STATUS.equals(order.getStatus())) {
            return Optional.of(order);
        }

        order.setStatus(CLOSED_STATUS);
        order.setClosePrice(closePrice);
        order.setClosedAt(Instant.now());
        tradeExecutionService.execute(tradeExecutionRequest(order, opposite(order.getSide()), TradeExecutionType.CLOSE, closePrice));
        return Optional.of(order);
    }

    public Optional<OrderEntity> getOrderForUser(long userId, long orderId) {
        return orderDao.findById(orderId)
            .filter(order -> order.getUserId() != null && order.getUserId() == userId);
    }

    private TradeExecutionRequest tradeExecutionRequest(
        OrderEntity order,
        OrderEntity.Side side,
        TradeExecutionType executionType,
        double price
    ) {
        return new TradeExecutionRequest(
            order.getUserId(),
            order.getId(),
            order.getSymbol(),
            toTradeSide(side),
            executionType,
            order.getQuantity(),
            price
        );
    }

    private TradeSide toTradeSide(OrderEntity.Side side) {
        if (side == null) {
            throw new IllegalArgumentException("order side is required");
        }
        return TradeSide.valueOf(side.name());
    }

    private OrderEntity.Side opposite(OrderEntity.Side side) {
        if (side == null) {
            throw new IllegalArgumentException("order side is required");
        }
        return side == OrderEntity.Side.BUY ? OrderEntity.Side.SELL : OrderEntity.Side.BUY;
    }
}
