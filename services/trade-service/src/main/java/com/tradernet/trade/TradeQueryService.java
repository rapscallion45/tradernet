package com.tradernet.trade;

import com.tradernet.trade.dto.TradeResponseDto;
import jakarta.ejb.Local;

import java.util.List;

/**
 * Application contract for user-scoped trade history queries.
 */
@Local
public interface TradeQueryService {

    List<TradeResponseDto> getTradesForUser(long userId, String symbol);
}
