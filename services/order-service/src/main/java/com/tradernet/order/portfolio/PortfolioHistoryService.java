package com.tradernet.order.portfolio;

import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.currencyconversion.CurrencyConversionService;
import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.order.MarketPriceService;
import com.tradernet.order.dto.PortfolioHistoryEventDto;
import com.tradernet.order.dto.PortfolioHistoryPointDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import static com.tradernet.order.CurrencyRounding.roundCurrency;

/**
 * Builds bounded daily portfolio history from signed position events.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PortfolioHistoryService {

    private static final int MAX_HISTORY_DAYS = 1_000;

    @EJB
    private PortfolioPositionService positionService;

    @EJB
    private MarketAiService marketAiService;

    @EJB
    private MarketPriceService marketPriceService;

    @EJB
    private CurrencyConversionService currencyConversionService;

    public void prefetchConversionRates(
        List<PortfolioPositionEvent> events,
        CurrencyCode displayCurrency,
        Instant now
    ) {
        if (events.isEmpty()) {
            return;
        }
        final Instant earliest = events.get(0).getTimestamp();
        final Instant historyStart = now.minus(MAX_HISTORY_DAYS - 1L, ChronoUnit.DAYS);
        final Instant boundedStart = earliest.isBefore(historyStart) ? historyStart : earliest;
        events.stream()
            .map(event -> currencyConversionService.resolveQuoteCurrency(event.getSymbol()))
            .distinct()
            .filter(sourceCurrency -> sourceCurrency != displayCurrency)
            .forEach(sourceCurrency -> currencyConversionService.prefetchRates(
                sourceCurrency,
                displayCurrency,
                boundedStart,
                now
            ));
    }

    public List<PortfolioHistoryPointDto> buildHistory(
        List<PortfolioPositionEvent> events,
        CurrencyCode displayCurrency,
        Instant now
    ) {
        if (events.isEmpty()) {
            return List.of();
        }

        final Map<String, PortfolioPosition> rollingPositions = new HashMap<>();
        final Map<LocalDate, List<PortfolioPositionEvent>> eventsByDate = new HashMap<>();
        final Set<String> symbols = new HashSet<>();
        final LocalDate today = toUtcDate(now);
        LocalDate firstDate = toUtcDate(events.get(0).getTimestamp());
        if (firstDate.isAfter(today)) {
            firstDate = today;
        }

        final long requestedDays = ChronoUnit.DAYS.between(firstDate, today) + 1L;
        final int historyDays = (int) Math.max(1L, Math.min(requestedDays, MAX_HISTORY_DAYS));
        final LocalDate startDate = today.minusDays(historyDays - 1L);

        for (PortfolioPositionEvent event : events) {
            final LocalDate eventDate = toUtcDate(event.getTimestamp());
            symbols.add(event.getSymbol());
            eventsByDate.computeIfAbsent(eventDate, ignored -> new ArrayList<>()).add(event);
            if (eventDate.isBefore(startDate)) {
                positionService.applyEvent(rollingPositions, event);
            }
        }

        final Map<String, NavigableMap<LocalDate, Double>> dailyPrices = loadDailyClosePrices(
            symbols,
            historyDays + 5
        );
        final List<PortfolioHistoryPointDto> history = new ArrayList<>(historyDays);
        for (int offset = 0; offset < historyDays; offset += 1) {
            final LocalDate date = startDate.plusDays(offset);
            final List<PortfolioPositionEvent> dailyEvents = eventsByDate.getOrDefault(date, List.of());
            dailyEvents.forEach(event -> positionService.applyEvent(rollingPositions, event));
            final boolean todayPoint = date.equals(today);
            final Instant valuationTime = todayPoint ? now : date.atStartOfDay(ZoneOffset.UTC).toInstant();
            history.add(new PortfolioHistoryPointDto(
                valuationTime.toEpochMilli(),
                roundCurrency(accountValue(
                    rollingPositions,
                    displayCurrency,
                    valuationTime,
                    date,
                    todayPoint,
                    dailyPrices
                )),
                historyEvents(dailyEvents, displayCurrency)
            ));
        }
        return history;
    }

    private double accountValue(
        Map<String, PortfolioPosition> positions,
        CurrencyCode displayCurrency,
        Instant timestamp,
        LocalDate valuationDate,
        boolean useLivePrice,
        Map<String, NavigableMap<LocalDate, Double>> dailyPrices
    ) {
        double totalValue = 0.0;
        for (Map.Entry<String, PortfolioPosition> entry : positions.entrySet()) {
            final PortfolioPosition position = entry.getValue();
            if (position.isFlat()) {
                continue;
            }
            final String symbol = entry.getKey();
            final double fallbackPrice = position.getLastKnownPrice() > 0.0
                ? position.getLastKnownPrice()
                : position.getAverageCost();
            final double rawPrice = useLivePrice
                ? marketPriceService.resolveCurrentPrice(symbol, fallbackPrice)
                : historicalPrice(symbol, valuationDate, fallbackPrice, dailyPrices);
            final CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(symbol);
            totalValue += position.getNetQuantity() * currencyConversionService.convertAmount(
                rawPrice,
                sourceCurrency,
                displayCurrency,
                timestamp
            );
        }
        return totalValue;
    }

    private List<PortfolioHistoryEventDto> historyEvents(
        List<PortfolioPositionEvent> events,
        CurrencyCode displayCurrency
    ) {
        final List<PortfolioHistoryEventDto> result = new ArrayList<>(events.size());
        for (PortfolioPositionEvent event : events) {
            final CurrencyCode sourceCurrency = currencyConversionService.resolveQuoteCurrency(event.getSymbol());
            final double convertedPrice = currencyConversionService.convertAmount(
                event.getPrice(),
                sourceCurrency,
                displayCurrency,
                event.getTimestamp()
            );
            result.add(new PortfolioHistoryEventDto(
                event.getSymbol(),
                event.getSide().name(),
                Math.abs(event.getQuantityDelta()),
                roundCurrency(convertedPrice),
                event.getTimestamp().toEpochMilli()
            ));
        }
        return result;
    }

    private Map<String, NavigableMap<LocalDate, Double>> loadDailyClosePrices(
        Set<String> symbols,
        int requestedDays
    ) {
        final Map<String, NavigableMap<LocalDate, Double>> pricesBySymbol = new HashMap<>();
        final int limit = Math.max(1, Math.min(requestedDays, MAX_HISTORY_DAYS));
        for (String symbol : symbols) {
            final NavigableMap<LocalDate, Double> closesByDate = new TreeMap<>();
            for (MarketBar bar : marketAiService.getBars(symbol, "1D", limit)) {
                if (bar != null && bar.getClose() > 0.0) {
                    closesByDate.put(toUtcDate(Instant.ofEpochMilli(bar.getBucketStart())), bar.getClose());
                }
            }
            pricesBySymbol.put(symbol, closesByDate);
        }
        return pricesBySymbol;
    }

    private double historicalPrice(
        String symbol,
        LocalDate valuationDate,
        double fallbackPrice,
        Map<String, NavigableMap<LocalDate, Double>> dailyPrices
    ) {
        final NavigableMap<LocalDate, Double> prices = dailyPrices.get(symbol);
        if (prices == null || prices.isEmpty()) {
            return fallbackPrice;
        }
        final Map.Entry<LocalDate, Double> close = prices.floorEntry(valuationDate);
        return close == null || close.getValue() == null || close.getValue() <= 0.0
            ? fallbackPrice
            : close.getValue();
    }

    private LocalDate toUtcDate(Instant instant) {
        return LocalDate.ofInstant(instant, ZoneOffset.UTC);
    }
}
