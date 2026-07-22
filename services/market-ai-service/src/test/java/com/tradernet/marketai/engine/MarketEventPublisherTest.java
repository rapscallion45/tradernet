package com.tradernet.marketai.engine;

import com.tradernet.marketai.model.MarketBar;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketEventPublisherTest {

    @Test
    void dispatchesBarsOnlyToListenersForThePublishedSymbol() throws Exception {
        final MarketEventPublisher publisher = new MarketEventPublisher();
        final AtomicInteger bitcoinEvents = new AtomicInteger();
        final AtomicInteger etherEvents = new AtomicInteger();

        final AutoCloseable bitcoin = publisher.onBar("BTCUSDT", ignored -> bitcoinEvents.incrementAndGet());
        final AutoCloseable ether = publisher.onBar("ETHUSDT", ignored -> etherEvents.incrementAndGet());
        try {
            publisher.publishBar(new MarketBar("BTCUSDT", 1L, 1.0, 1.0, 1.0, 1.0, 1.0, false));
        } finally {
            bitcoin.close();
            ether.close();
        }

        assertEquals(1, bitcoinEvents.get());
        assertEquals(0, etherEvents.get());
    }
}
