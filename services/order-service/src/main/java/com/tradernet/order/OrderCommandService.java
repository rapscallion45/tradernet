package com.tradernet.order;

import jakarta.ejb.Local;

import java.util.Optional;

/**
 * Local command contract for persisted order workflows.
 */
@Local
public interface OrderCommandService {

    OrderRecord createOrder(long userId, CreateOrderCommand command);

    Optional<OrderRecord> closeOrder(long userId, long orderId, double closePrice);

    boolean updateMarketInsights(long orderId, String aiPrediction, Double bullScore);
}
