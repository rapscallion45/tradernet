package com.tradernet.marketai.orderbook;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Maps internal order-book state to the public market snapshot contract.
 */
final class OrderBookSnapshotFactory {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    OrderBookSnapshot create(
        String symbol,
        NavigableMap<BigDecimal, BigDecimal> bids,
        NavigableMap<BigDecimal, BigDecimal> asks,
        int requestedLevels,
        boolean running,
        boolean synchronizedStream,
        long lastUpdateId,
        long lastExchangeEventTimeMs,
        long lastAppliedAtMs,
        long staleAfterMs,
        long resyncCount,
        int exchangeSnapshotLimit,
        String lastError
    ) {
        final int levels = Math.max(1, Math.min(200, requestedLevels));
        final List<OrderBookLevel> bidLevels = buildLevels(bids, levels);
        final List<OrderBookLevel> askLevels = buildLevels(asks, levels);
        final long now = System.currentTimeMillis();
        final boolean hasData = !bidLevels.isEmpty() || !askLevels.isEmpty();
        final boolean stale = hasData && lastAppliedAtMs > 0L && now - lastAppliedAtMs > staleAfterMs;
        final OrderBookStatus status = status(hasData, stale, running, synchronizedStream, lastError);
        final double bestBid = bidLevels.isEmpty() ? 0.0 : bidLevels.get(0).getPrice();
        final double bestAsk = askLevels.isEmpty() ? 0.0 : askLevels.get(0).getPrice();
        final double midPrice = bestBid > 0.0 && bestAsk > 0.0 ? (bestBid + bestAsk) / 2.0 : 0.0;
        final double spread = bestBid > 0.0 && bestAsk > 0.0 ? Math.max(0.0, bestAsk - bestBid) : 0.0;
        final double bidDepth = sumNotional(bidLevels);
        final double askDepth = sumNotional(askLevels);
        final double totalDepth = bidDepth + askDepth;

        return new OrderBookSnapshot(
            symbol,
            quoteCurrency(symbol),
            status,
            "binance-spot",
            "AGGREGATED_L2",
            statusMessage(status, lastError),
            lastExchangeEventTimeMs,
            lastUpdateId,
            lastExchangeEventTimeMs > 0L ? Math.max(0L, now - lastExchangeEventTimeMs) : 0L,
            resyncCount,
            exchangeSnapshotLimit,
            levels,
            stale,
            bestBid,
            bestAsk,
            midPrice,
            spread,
            midPrice > 0.0 ? (spread / midPrice) * 100.0 : 0.0,
            bidDepth,
            askDepth,
            totalDepth > 0.0 ? ((bidDepth - askDepth) / totalDepth) * 100.0 : 0.0,
            bidLevels,
            askLevels
        );
    }

    private List<OrderBookLevel> buildLevels(NavigableMap<BigDecimal, BigDecimal> levels, int limit) {
        final List<LevelDraft> drafts = new ArrayList<>();
        BigDecimal cumulativeQuantity = BigDecimal.ZERO;
        BigDecimal cumulativeNotional = BigDecimal.ZERO;
        int count = 0;
        for (Map.Entry<BigDecimal, BigDecimal> entry : levels.entrySet()) {
            if (count++ >= limit) {
                break;
            }
            final BigDecimal notional = entry.getKey().multiply(entry.getValue(), MC);
            cumulativeQuantity = cumulativeQuantity.add(entry.getValue(), MC);
            cumulativeNotional = cumulativeNotional.add(notional, MC);
            drafts.add(new LevelDraft(
                entry.getKey(),
                entry.getValue(),
                notional,
                cumulativeQuantity,
                cumulativeNotional
            ));
        }
        if (drafts.isEmpty()) {
            return List.of();
        }

        final BigDecimal totalNotional = drafts.get(drafts.size() - 1).cumulativeNotional;
        final List<OrderBookLevel> result = new ArrayList<>(drafts.size());
        for (LevelDraft draft : drafts) {
            final double depthPercent = totalNotional.signum() == 0
                ? 0.0
                : draft.cumulativeNotional.multiply(ONE_HUNDRED, MC).divide(totalNotional, MC).doubleValue();
            result.add(new OrderBookLevel(
                draft.price.doubleValue(),
                draft.quantity.doubleValue(),
                draft.notional.doubleValue(),
                draft.cumulativeQuantity.doubleValue(),
                draft.cumulativeNotional.doubleValue(),
                depthPercent
            ));
        }
        return result;
    }

    private double sumNotional(List<OrderBookLevel> levels) {
        return levels.stream().mapToDouble(OrderBookLevel::getNotional).sum();
    }

    private OrderBookStatus status(
        boolean hasData,
        boolean stale,
        boolean running,
        boolean synchronizedStream,
        String lastError
    ) {
        if (synchronizedStream && running && !stale) {
            return OrderBookStatus.LIVE;
        }
        if (synchronizedStream && running) {
            return OrderBookStatus.STALE;
        }
        if (hasData && !running) {
            return OrderBookStatus.SNAPSHOT_ONLY;
        }
        if (hasData) {
            return OrderBookStatus.SYNCING;
        }
        return lastError == null ? OrderBookStatus.SYNCING : OrderBookStatus.UNAVAILABLE;
    }

    private String statusMessage(OrderBookStatus status, String lastError) {
        switch (status) {
            case LIVE:
                return "Live Binance aggregated L2 depth stream";
            case STALE:
                return "Order book stream has not applied an update recently";
            case SNAPSHOT_ONLY:
                return lastError == null
                    ? "REST snapshot available while live stream reconnects"
                    : "REST snapshot only: " + lastError;
            case UNAVAILABLE:
                return lastError == null ? "Order book unavailable" : lastError;
            case SYNCING:
            default:
                return lastError == null ? "Syncing Binance depth stream with REST snapshot" : lastError;
        }
    }

    private String quoteCurrency(String symbol) {
        if (symbol.endsWith("USDT") || symbol.endsWith("USD")) {
            return "USD";
        }
        if (symbol.endsWith("EUR")) {
            return "EUR";
        }
        if (symbol.endsWith("GBP")) {
            return "GBP";
        }
        return "USD";
    }

    private static final class LevelDraft {
        private final BigDecimal price;
        private final BigDecimal quantity;
        private final BigDecimal notional;
        private final BigDecimal cumulativeQuantity;
        private final BigDecimal cumulativeNotional;

        private LevelDraft(
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal notional,
            BigDecimal cumulativeQuantity,
            BigDecimal cumulativeNotional
        ) {
            this.price = price;
            this.quantity = quantity;
            this.notional = notional;
            this.cumulativeQuantity = cumulativeQuantity;
            this.cumulativeNotional = cumulativeNotional;
        }
    }
}
