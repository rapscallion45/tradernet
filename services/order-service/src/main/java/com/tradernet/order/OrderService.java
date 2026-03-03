package com.tradernet.order;

import com.tradernet.jpa.dao.OrderDao;
import com.tradernet.jpa.entities.OrderEntity;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;

/**
 * Persists mocked orders and exposes user-level order retrieval.
 */
@Stateless
public class OrderService {

    private static final String MOCKED_STATUS = "MOCKED_ACCEPTED";

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
        order.setStatus(MOCKED_STATUS);
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
}
