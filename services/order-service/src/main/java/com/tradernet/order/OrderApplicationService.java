package com.tradernet.order;

import com.tradernet.order.dto.OrderRequestDto;
import com.tradernet.order.dto.OrderResponseDto;
import jakarta.ejb.Local;

import java.util.List;
import java.util.Optional;

/**
 * API-facing contract for user-scoped order workflows.
 */
@Local
public interface OrderApplicationService {

    List<OrderResponseDto> getOrdersForUser(long userId, String currency);

    OrderResponseDto createOrder(long userId, OrderRequestDto request);

    Optional<OrderResponseDto> closeOrder(long userId, long orderId);
}
