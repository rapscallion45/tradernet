package com.tradernet.order;

import com.tradernet.marketai.MarketAiService;
import com.tradernet.domain.market.MarketSymbolNormalizer;
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
    @EJB
    private MarketAiService marketAiService;

    @EJB
    private OrderService orderService;

    @EJB
    private OrderConfiguration configuration;

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
            return roundCurrency(marketAiService.getBullScore(symbol, configuration.getBullScoreHorizonDays()));
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
