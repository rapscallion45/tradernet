package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.OrderEntity;

import java.util.List;

public interface OrderDao {
    void save(OrderEntity order);
    List<OrderEntity> findAll();
    List<OrderEntity> findByUserId(long userId);
    List<OrderEntity> findBySymbol(String symbol);
    void deleteAll();
}
