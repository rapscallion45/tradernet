package com.tradernet.order;

import jakarta.ejb.Local;

import java.util.List;
import java.util.Optional;

/**
 * Local query contract for user-scoped orders.
 */
@Local
public interface OrderQueryService {

    List<OrderRecord> getOrdersByUserId(long userId);

    Optional<OrderRecord> getOrderForUser(long userId, long orderId);
}
