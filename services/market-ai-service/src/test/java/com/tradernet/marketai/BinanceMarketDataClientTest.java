package com.tradernet.marketai;

import com.tradernet.marketai.model.ChartInterval;
import com.tradernet.marketai.model.MarketBar;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BinanceMarketDataClientTest {

    @Test
    void pagesBackwardWhenTheRequestedHistoryExceedsTheExchangePageLimit() {
        final StubBinanceMarketDataClient client = new StubBinanceMarketDataClient();

        final List<MarketBar> bars = client.fetchKlines(
            "BTCUSDT",
            ChartInterval.parse("1H"),
            MarketBarProvider.MAX_BARS
        );

        assertEquals(2_000, bars.size());
        assertEquals(0L, bars.get(0).getBucketStart());
        assertEquals(1_999_000L, bars.get(bars.size() - 1).getBucketStart());
        assertEquals(2, client.endTimes.size());
        assertNull(client.endTimes.get(0));
        assertEquals(999_999L, client.endTimes.get(1));
    }

    private static final class StubBinanceMarketDataClient extends BinanceMarketDataClient {
        private final List<Long> endTimes = new ArrayList<>();

        @Override
        protected List<MarketBar> fetchKlinePage(
            String normalizedSymbol,
            ChartInterval interval,
            int limit,
            Long endTime
        ) {
            endTimes.add(endTime);
            final long firstBucketStart = endTime == null ? 1_000_000L : 0L;
            final List<MarketBar> bars = new ArrayList<>(limit);
            for (int index = 0; index < limit; index++) {
                bars.add(new MarketBar(
                    normalizedSymbol,
                    firstBucketStart + index * 1_000L,
                    1.0,
                    2.0,
                    0.5,
                    1.5,
                    10.0,
                    true
                ));
            }
            return bars;
        }
    }
}
