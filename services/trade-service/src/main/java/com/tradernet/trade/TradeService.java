package com.tradernet.trade;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.jpa.dao.TradeDao;
import com.tradernet.jpa.entities.TradeEntity;
import com.tradernet.trade.dto.TradeResponseDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements the trade command and user-scoped query contracts.
 */
@Stateless
public class TradeService implements TradeCommandService, TradeQueryService {

    private static final String CLOSE_EXECUTION_TYPE = TradeExecutionType.CLOSE.name();

    @EJB
    private TradeDao tradeDao;

    @Override
    public void execute(TradeExecutionRequest request) {
        requireRequest(request);
        if (request.getExecutionType() == TradeExecutionType.CLOSE
            && request.getOrderId() != null
            && tradeDao.existsByOrderIdAndExecutionType(request.getOrderId(), CLOSE_EXECUTION_TYPE)) {
            return;
        }

        TradeEntity trade = new TradeEntity(
            request.getUserId(),
            request.getOrderId(),
            MarketSymbolNormalizer.normalizeSymbol(request.getSymbol()),
            request.getSide().name(),
            request.getExecutionType().name(),
            signedQuantity(request.getSide(), request.getQuantity()),
            request.getPrice()
        );
        tradeDao.save(trade);
    }

    @Override
    public List<TradeResponseDto> getTradesForUser(long userId, String symbol) {
        List<TradeEntity> trades;
        if (symbol == null || symbol.isBlank()) {
            trades = tradeDao.findByUserId(userId);
        } else {
            trades = tradeDao.findByUserIdAndSymbol(userId, MarketSymbolNormalizer.normalizeSymbol(symbol));
        }
        return trades.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    private TradeResponseDto toResponse(TradeEntity trade) {
        TradeResponseDto dto = new TradeResponseDto();
        dto.setId(trade.getId());
        dto.setOrderId(trade.getOrderId());
        dto.setSymbol(trade.getSymbol());
        dto.setSide(trade.getSide());
        dto.setExecutionType(trade.getExecutionType());
        dto.setQuantity(trade.getQuantity());
        dto.setPrice(trade.getPrice());
        dto.setTimestamp(trade.getTimestamp());
        return dto;
    }

    private void requireRequest(TradeExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("trade execution request is required");
        }
        if (request.getSymbol() == null || request.getSymbol().isBlank()) {
            throw new IllegalArgumentException("trade symbol is required");
        }
        if (request.getSide() == null) {
            throw new IllegalArgumentException("trade side is required");
        }
        if (request.getExecutionType() == null) {
            throw new IllegalArgumentException("trade execution type is required");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("trade user id is required");
        }
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("trade quantity must be greater than 0");
        }
        if (request.getPrice() <= 0) {
            throw new IllegalArgumentException("trade price must be greater than 0");
        }
    }

    private double signedQuantity(TradeSide side, double quantity) {
        return side == TradeSide.SELL ? -quantity : quantity;
    }
}
