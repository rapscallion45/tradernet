package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.OrderEntity;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) interface for orders.
 * Defines methods for interacting with the persistence layer.
 */
public interface OrderDao {

    /**
     * Saves a new order to the database.
     *
     * @param order order to save
     */
    void save(OrderEntity order);

    /**
     * Retrieves all orders from the database.
     *
     * @return list of all orders
     */
    List<OrderEntity> findAll();

    /**
     * Retrieves orders for a specific user.
     *
     * @param userId user identifier
     * @return list of user orders
     */
    List<OrderEntity> findByUserId(long userId);

    /**
     * Retrieves orders for a specific symbol.
     *
     * @param symbol stock symbol
     * @return list of orders for the symbol
     */
    List<OrderEntity> findBySymbol(String symbol);

    /**
     * Retrieves an order by id.
     *
     * @param orderId order id
     * @return optional order
     */
    Optional<OrderEntity> findById(long orderId);

    /**
     * Deletes all orders (useful for testing or resetting state).
     */
    void deleteAll();
}
