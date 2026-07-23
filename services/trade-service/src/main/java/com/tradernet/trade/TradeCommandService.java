package com.tradernet.trade;

import jakarta.ejb.Local;

/**
 * Application contract for recording trade executions.
 */
@Local
public interface TradeCommandService {

    void execute(TradeExecutionRequest request);
}
