package com.tradernet.order;

import com.tradernet.jpa.entities.OrderEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for managing orders in the system.
 */
public class OrderBookService {

    // Stores all orders in memory
    private final List<OrderEntity> orders = new ArrayList<>();

    /**
     * Adds a new order to the order book.
     *
     * @param order the order to add
     */
    public boolean addOrder(OrderEntity order) {
        if (!isValidOrder(order)) {
            throw new IllegalArgumentException("Invalid order: " + order);
        }
        return orders.add(order);
    }

    /**
     * Validates an order.
     *
     * @param order the order to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidOrder(OrderEntity order) {
        return order.getQuantity() > 0 && order.getSymbol() != null && !order.getSymbol().isEmpty();
    }

    /**
     * Returns a copy of all orders.
     *
     * @return list of orders
     */
    public List<OrderEntity> getOrders() {
        return new ArrayList<>(orders);
    }

    /**
     * Clears all orders from the order book.
     */
    public void clearOrders() {
        orders.clear();
    }
}
