package com.tradernet.order;

import com.tradernet.model.Order;
import jakarta.ejb.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class OrderService {
    private final List<Order> orders = new ArrayList<>();

    public void createOrder(Order order) { orders.add(order); }

    public List<Order> getOrders() { return orders; }
}
