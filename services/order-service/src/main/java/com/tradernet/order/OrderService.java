package com.tradernet.order;

import com.tradernet.jpa.dao.OrderDao;
import com.tradernet.jpa.entities.OrderEntity;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persists mocked orders and exposes user-level order retrieval.
 */
@Stateless
public class OrderService {

    public static final String OPEN_STATUS = "OPEN";
    public static final String CLOSED_STATUS = "CLOSED";

    @Inject
    private OrderDao orderDao;

    /**
     * Creates and persists a mocked order linked to a user.
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
        return order;
    }

    /**
     * Returns all orders sorted by creation time descending.
     */
    public List<OrderEntity> getOrders() {
        return orderDao.findAll();
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
        Optional<OrderEntity> foundOrder = getOrderForUser(userId, orderId);

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
        return Optional.of(order);
    }

    public Optional<OrderEntity> getOrderForUser(long userId, long orderId) {
        return orderDao.findById(orderId)
            .filter(order -> order.getUserId() != null && order.getUserId() == userId);
    }
}
