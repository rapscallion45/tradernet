package com.tradernet.order;

import com.tradernet.jpa.dao.OrderDao;
import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.order.dto.OrderSide;
import com.tradernet.trade.TradeCommandService;
import com.tradernet.trade.TradeExecutionRequest;
import com.tradernet.trade.TradeExecutionType;
import com.tradernet.trade.TradeSide;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persists orders and records trade fills behind persistence-neutral contracts.
 */
@Stateless
public class OrderService implements OrderCommandService, OrderQueryService, OrderPortfolioQueryService {

    private static final String OPEN_STATUS = OrderStatus.OPEN.name();
    private static final String CLOSED_STATUS = OrderStatus.CLOSED.name();

    @EJB
    private OrderDao orderDao;

    @EJB
    private TradeCommandService tradeService;

    @Override
    public OrderRecord createOrder(long userId, CreateOrderCommand command) {
        final OrderEntity order = new OrderEntity(
            command.getSymbol(),
            command.getQuantity(),
            command.getPrice(),
            toEntitySide(command.getSide())
        );
        order.setUserId(userId);
        order.setStatus(OPEN_STATUS);
        order.setCreatedAt(Instant.now());
        orderDao.save(order);
        tradeService.execute(tradeExecutionRequest(order, order.getSide(), TradeExecutionType.OPEN, order.getPrice()));
        return toRecord(order);
    }

    @Override
    public List<OrderRecord> getOrdersByUserId(long userId) {
        return orderDao.findByUserId(userId).stream()
            .map(this::toRecord)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrderPortfolioItem> getPortfolioItems(long userId) {
        final List<OrderPortfolioItem> items = new ArrayList<>();
        for (OrderEntity order : orderDao.findByUserId(userId)) {
            items.add(new OrderPortfolioItem(
                order.getSymbol(),
                toOrderSide(order.getSide()),
                order.getQuantity(),
                order.getPrice(),
                CLOSED_STATUS.equals(order.getStatus()) && order.getClosePrice() != null,
                order.getCreatedAt(),
                order.getClosedAt(),
                order.getClosePrice()
            ));
        }
        return items;
    }

    @Override
    public Optional<OrderRecord> closeOrder(long userId, long orderId, double closePrice) {
        final Optional<OrderEntity> foundOrder = orderDao.findByIdForUpdate(orderId)
            .filter(order -> order.getUserId() != null && order.getUserId() == userId);
        if (foundOrder.isEmpty()) {
            return Optional.empty();
        }

        final OrderEntity order = foundOrder.get();
        if (!CLOSED_STATUS.equals(order.getStatus())) {
            order.setStatus(CLOSED_STATUS);
            order.setClosePrice(closePrice);
            order.setClosedAt(Instant.now());
            tradeService.execute(tradeExecutionRequest(
                order,
                opposite(order.getSide()),
                TradeExecutionType.CLOSE,
                closePrice
            ));
        }
        return Optional.of(toRecord(order));
    }

    @Override
    public Optional<OrderRecord> getOrderForUser(long userId, long orderId) {
        return orderDao.findById(orderId)
            .filter(order -> order.getUserId() != null && order.getUserId() == userId)
            .map(this::toRecord);
    }

    @Override
    public boolean updateMarketInsights(long orderId, String aiPrediction, Double bullScore) {
        return orderDao.findByIdForUpdate(orderId)
            .map(order -> {
                if (aiPrediction != null && !aiPrediction.isBlank()) {
                    order.setAiPrediction(aiPrediction);
                }
                if (bullScore != null) {
                    order.setBullScore(bullScore);
                }
                return true;
            })
            .orElse(false);
    }

    private OrderRecord toRecord(OrderEntity order) {
        return new OrderRecord(
            order.getId() == null ? 0L : order.getId(),
            order.getUserId() == null ? 0L : order.getUserId(),
            order.getSymbol(),
            toOrderSide(order.getSide()),
            order.getQuantity(),
            order.getPrice(),
            OrderStatus.valueOf(order.getStatus()),
            order.getCreatedAt(),
            order.getClosedAt(),
            order.getClosePrice(),
            order.getAiPrediction(),
            order.getBullScore()
        );
    }

    private TradeExecutionRequest tradeExecutionRequest(
        OrderEntity order,
        OrderEntity.Side side,
        TradeExecutionType executionType,
        double price
    ) {
        return new TradeExecutionRequest(
            order.getUserId(),
            order.getId(),
            order.getSymbol(),
            toTradeSide(side),
            executionType,
            order.getQuantity(),
            price
        );
    }

    private OrderEntity.Side toEntitySide(OrderSide side) {
        if (side == null) {
            throw new IllegalArgumentException("order side is required");
        }
        return OrderEntity.Side.valueOf(side.name());
    }

    private OrderSide toOrderSide(OrderEntity.Side side) {
        return side == null ? null : OrderSide.valueOf(side.name());
    }

    private TradeSide toTradeSide(OrderEntity.Side side) {
        if (side == null) {
            throw new IllegalArgumentException("order side is required");
        }
        return TradeSide.valueOf(side.name());
    }

    private OrderEntity.Side opposite(OrderEntity.Side side) {
        if (side == null) {
            throw new IllegalArgumentException("order side is required");
        }
        return side == OrderEntity.Side.BUY ? OrderEntity.Side.SELL : OrderEntity.Side.BUY;
    }
}
