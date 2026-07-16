package com.tradernet.trade;

import com.tradernet.jpa.dao.TradeDao;
import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.jpa.entities.TradeEntity;
import com.tradernet.marketai.MarketSymbolNormalizer;
import com.tradernet.trade.dto.TradeResponseDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Executes orders and persists the resulting trade fill.
 * In a real system this would integrate with a broker/exchange.
 */
@Stateless
public class TradeExecutionService {

    public static final String OPEN_EXECUTION_TYPE = "OPEN";
    public static final String CLOSE_EXECUTION_TYPE = "CLOSE";

    @EJB
    private TradeDao tradeDao;

    /**
     * Executes the given order.
     *
     * @param order order to execute
     */
    public void execute(OrderEntity order) {
        requireOrder(order);
        execute(order, order.getSide(), OPEN_EXECUTION_TYPE, order.getPrice());
    }

    /**
     * Executes the inverse trade required to close an order.
     *
     * @param order open order being closed
     * @param closePrice close execution price
     */
    public void executeClose(OrderEntity order, double closePrice) {
        requireOrder(order);
        if (order.getId() != null && tradeDao.existsByOrderIdAndExecutionType(order.getId(), CLOSE_EXECUTION_TYPE)) {
            return;
        }

        OrderEntity.Side closeSide = order.getSide() == OrderEntity.Side.BUY
            ? OrderEntity.Side.SELL
            : OrderEntity.Side.BUY;
        execute(order, closeSide, CLOSE_EXECUTION_TYPE, closePrice);
    }

    public List<TradeResponseDto> getTradesForUser(long userId, String symbol) {
        List<TradeEntity> trades;
        if (symbol == null || symbol.isBlank()) {
            trades = tradeDao.findByUserId(userId);
        } else {
            trades = tradeDao.findByUserIdAndSymbol(userId, MarketSymbolNormalizer.normalizeSymbol(symbol));
        }
        return trades.stream()
            .map(TradeResponseDto::fromTrade)
            .collect(Collectors.toList());
    }

    private void execute(OrderEntity order, OrderEntity.Side executionSide, String executionType, double executionPrice) {
        requireOrder(order);
        if (executionPrice <= 0) {
            throw new IllegalArgumentException("execution price must be greater than 0");
        }

        TradeEntity trade = new TradeEntity(
            order.getUserId(),
            order.getId(),
            MarketSymbolNormalizer.normalizeSymbol(order.getSymbol()),
            executionSide.name(),
            executionType,
            signedQuantity(executionSide, order.getQuantity()),
            executionPrice
        );
        tradeDao.save(trade);
    }

    private void requireOrder(OrderEntity order) {
        if (order == null) {
            throw new IllegalArgumentException("order is required");
        }
        if (order.getSymbol() == null || order.getSymbol().isBlank()) {
            throw new IllegalArgumentException("order symbol is required");
        }
        if (order.getSide() == null) {
            throw new IllegalArgumentException("order side is required");
        }
        if (order.getUserId() == null) {
            throw new IllegalArgumentException("order user id is required");
        }
        if (order.getQuantity() <= 0) {
            throw new IllegalArgumentException("order quantity must be greater than 0");
        }
    }

    private double signedQuantity(OrderEntity.Side side, double quantity) {
        return side == OrderEntity.Side.SELL ? -quantity : quantity;
    }

}
