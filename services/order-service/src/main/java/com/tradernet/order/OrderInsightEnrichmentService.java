package com.tradernet.order;

import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.MarketSignalProvider;
import com.tradernet.marketai.forecast.MarketForecastProvider;
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
    private MarketSignalProvider marketSignalProvider;

    @EJB
    private MarketForecastProvider marketForecastProvider;

    @EJB
    private OrderCommandService orderService;

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
            if (orderService.updateMarketInsights(orderId, aiPrediction, bullScore)) {
                LOG.debug("Updated market insights for order {}.", orderId);
            } else {
                LOG.debug("Skipped market insight update because order {} no longer exists.", orderId);
            }
        } catch (RuntimeException ex) {
            LOG.warn("Unable to persist market insights for order {}.", orderId, ex);
        }
    }

    private Double resolveBullScore(String symbol) {
        try {
            return roundCurrency(marketForecastProvider.getForecast(symbol, configuration.getBullScoreHorizonDays()).getBullScore());
        } catch (RuntimeException ex) {
            LOG.warn("Unable to enrich order with bull score for symbol {}.", symbol, ex);
            return null;
        }
    }

    private String resolveAiPrediction(String symbol) {
        try {
            final List<AiSignal> signals = marketSignalProvider.getSignals(symbol, 200);
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
