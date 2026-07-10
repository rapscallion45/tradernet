package com.tradernet.marketai.orderbook;

import java.util.List;

/**
 * Display-ready market depth snapshot for a single exchange symbol.
 */
public class OrderBookSnapshot {

    private final String symbol;
    private final String quoteCurrency;
    private final OrderBookStatus status;
    private final String source;
    private final String aggregation;
    private final String message;
    private final long eventTime;
    private final long lastUpdateId;
    private final long updateLatencyMs;
    private final long resyncCount;
    private final int exchangeSnapshotLimit;
    private final int requestedLevels;
    private final boolean stale;
    private final double bestBid;
    private final double bestAsk;
    private final double midPrice;
    private final double spread;
    private final double spreadPercent;
    private final double bidDepthNotional;
    private final double askDepthNotional;
    private final double depthImbalancePercent;
    private final List<OrderBookLevel> bids;
    private final List<OrderBookLevel> asks;

    public OrderBookSnapshot(
            String symbol,
            String quoteCurrency,
            OrderBookStatus status,
            String source,
            String aggregation,
            String message,
            long eventTime,
            long lastUpdateId,
            long updateLatencyMs,
            long resyncCount,
            int exchangeSnapshotLimit,
            int requestedLevels,
            boolean stale,
            double bestBid,
            double bestAsk,
            double midPrice,
            double spread,
            double spreadPercent,
            double bidDepthNotional,
            double askDepthNotional,
            double depthImbalancePercent,
            List<OrderBookLevel> bids,
            List<OrderBookLevel> asks) {
        this.symbol = symbol;
        this.quoteCurrency = quoteCurrency;
        this.status = status;
        this.source = source;
        this.aggregation = aggregation;
        this.message = message;
        this.eventTime = eventTime;
        this.lastUpdateId = lastUpdateId;
        this.updateLatencyMs = updateLatencyMs;
        this.resyncCount = resyncCount;
        this.exchangeSnapshotLimit = exchangeSnapshotLimit;
        this.requestedLevels = requestedLevels;
        this.stale = stale;
        this.bestBid = bestBid;
        this.bestAsk = bestAsk;
        this.midPrice = midPrice;
        this.spread = spread;
        this.spreadPercent = spreadPercent;
        this.bidDepthNotional = bidDepthNotional;
        this.askDepthNotional = askDepthNotional;
        this.depthImbalancePercent = depthImbalancePercent;
        this.bids = List.copyOf(bids);
        this.asks = List.copyOf(asks);
    }

    public String getSymbol() {
        return symbol;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public OrderBookStatus getStatus() {
        return status;
    }

    public String getSource() {
        return source;
    }

    public String getAggregation() {
        return aggregation;
    }

    public String getMessage() {
        return message;
    }

    public long getEventTime() {
        return eventTime;
    }

    public long getLastUpdateId() {
        return lastUpdateId;
    }

    public long getUpdateLatencyMs() {
        return updateLatencyMs;
    }

    public long getResyncCount() {
        return resyncCount;
    }

    public int getExchangeSnapshotLimit() {
        return exchangeSnapshotLimit;
    }

    public int getRequestedLevels() {
        return requestedLevels;
    }

    public boolean isStale() {
        return stale;
    }

    public double getBestBid() {
        return bestBid;
    }

    public double getBestAsk() {
        return bestAsk;
    }

    public double getMidPrice() {
        return midPrice;
    }

    public double getSpread() {
        return spread;
    }

    public double getSpreadPercent() {
        return spreadPercent;
    }

    public double getBidDepthNotional() {
        return bidDepthNotional;
    }

    public double getAskDepthNotional() {
        return askDepthNotional;
    }

    public double getDepthImbalancePercent() {
        return depthImbalancePercent;
    }

    public List<OrderBookLevel> getBids() {
        return bids;
    }

    public List<OrderBookLevel> getAsks() {
        return asks;
    }
}
