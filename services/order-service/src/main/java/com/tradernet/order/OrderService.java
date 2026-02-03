package com.tradernet.order;

import com.tradernet.jpa.entities.OrderEntity;
import jakarta.ejb.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class OrderService {
    private final List<OrderEntity> orders = new ArrayList<>();

    public void createOrder(OrderEntity order) { orders.add(order); }

    public List<OrderEntity> getOrders() { return orders; }
}
