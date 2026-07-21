package com.tradernet.marketai;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.jpa.common.exception.DataAccessException;
import com.tradernet.jpa.dao.MarketBarDao;
import com.tradernet.marketai.model.MarketBar;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Maps closed market bars onto asynchronous durable storage operations.
 */
@Stateless
public class MarketBarStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(MarketBarStorageService.class);
    private static final String LIVE_STREAM_SOURCE = "binance-trade-stream";

    @EJB
    private MarketBarDao marketBarDao;

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void storeAsync(MarketBar bar) {
        if (bar == null) {
            return;
        }

        final String symbol = MarketSymbolNormalizer.normalizeSymbol(bar.getSymbol());
        final Instant bucket = Instant.ofEpochMilli(bar.getBucketStart());
        try {
            marketBarDao.insert(
                symbol,
                bucket,
                bar.getOpen(),
                bar.getHigh(),
                bar.getLow(),
                bar.getClose(),
                bar.getVolume(),
                LIVE_STREAM_SOURCE
            );
        } catch (DataAccessException ex) {
            LOG.error("Failed to persist closed market bar for {} at {}", symbol, bucket, ex);
        }
    }
}
