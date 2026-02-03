package com.tradernet.dao;

import com.tradernet.entities.OrderEntity;

import java.util.List;

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
     * Retrieves orders for a specific symbol.
     *
     * @param symbol stock symbol
     * @return list of orders for the symbol
     */
    List<OrderEntity> findBySymbol(String symbol);

    /**
     * Deletes all orders (useful for testing or resetting state).
     */
    void deleteAll();
}
