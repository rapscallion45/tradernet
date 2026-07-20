package com.tradernet.order;

import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.MarketSymbolNormalizer;
import com.tradernet.marketai.model.AiSignal;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

import static com.tradernet.order.CurrencyRounding.roundCurrency;

/**
 * Captures non-critical market advisory fields after order persistence.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OrderInsightEnrichmentService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderInsightEnrichmentService.class);
    private static final int DEFAULT_ORDER_BULL_SCORE_HORIZON_DAYS = 1;

    @EJB
    private MarketAiService marketAiService;

    @EJB
    private OrderService orderService;

    @Asynchronous
    public void enrichOrder(long orderId, String symbol) {
        final String normalizedSymbol = MarketSymbolNormalizer.normalizeSymbol(symbol);
        if (orderId <= 0 || normalizedSymbol.isBlank()) {
            return;
        }

        final String aiPrediction = resolveAiPrediction(normalizedSymbol);
        final Double bullScore = resolveBullScore(normalizedSymbol);
        if (aiPrediction == null && bullScore == null) {
            return;
        }

        try {
            orderService.updateMarketInsights(orderId, aiPrediction, bullScore)
                .ifPresentOrElse(
                    ignored -> LOG.debug("Updated market insights for order {}.", orderId),
                    () -> LOG.debug("Skipped market insight update because order {} no longer exists.", orderId)
                );
        } catch (RuntimeException ex) {
            LOG.warn("Unable to persist market insights for order {}.", orderId, ex);
        }
    }

    private Double resolveBullScore(String symbol) {
        try {
            final int horizonDays = Integer.parseInt(System.getProperty(
                "market.ai.orderBullScoreHorizonDays",
                String.valueOf(DEFAULT_ORDER_BULL_SCORE_HORIZON_DAYS)
            ));
            return roundCurrency(marketAiService.getBullScore(symbol, horizonDays));
        } catch (RuntimeException ex) {
            LOG.warn("Unable to enrich order with bull score for symbol {}.", symbol, ex);
            return null;
        }
    }

    private String resolveAiPrediction(String symbol) {
        try {
            final List<AiSignal> signals = marketAiService.getSignals(symbol, 200);
            if (signals == null || signals.isEmpty()) {
                return "HOLD";
            }

            return signals.stream()
                .filter(signal -> signal != null && signal.getSide() != null)
                .max(Comparator.comparingLong(AiSignal::getEventTime))
                .map(signal -> signal.getSide().name())
                .orElse("HOLD");
        } catch (RuntimeException ex) {
            LOG.warn("Unable to enrich order with AI prediction for symbol {}.", symbol, ex);
            return null;
        }
    }
}
