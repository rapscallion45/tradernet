package com.tradernet.api.resources;

import com.tradernet.currencyconversion.CurrencyConversionProvider;
import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.domain.market.MarketSymbolNormalizer;
import com.tradernet.marketai.MarketBarProvider;
import com.tradernet.marketai.MarketDataViewProvider;
import com.tradernet.marketai.MarketSignalProvider;
import com.tradernet.marketai.MarketSymbolProvider;
import com.tradernet.marketai.context.MarketContextOperations;
import com.tradernet.marketai.forecast.MarketForecastProvider;
import com.tradernet.marketai.forecast.MarketForecast;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.ChartInterval;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.marketai.model.MarketContextSnapshot;
import com.tradernet.marketai.model.MarketContextUpdateRequest;
import com.tradernet.marketai.orderbook.OrderBookSnapshot;
import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * REST API for chart bars and generated AI signals.
 */
@Path("/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MarketResource {

    @EJB
    private MarketSymbolProvider marketSymbolProvider;

    @EJB
    private MarketSignalProvider marketSignalProvider;

    @EJB
    private MarketContextOperations marketContextService;

    @EJB
    private MarketForecastProvider marketForecastProvider;

    @EJB
    private CurrencyConversionProvider currencyConversionService;

    @EJB
    private MarketDataViewProvider marketDataViewService;

    @GET
    @Path("/bars")
    public List<MarketBar> getBars(
            @NotBlank @Pattern(regexp = MarketSymbolNormalizer.VALIDATION_PATTERN, message = "symbol is invalid")
            @DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol,
            @NotBlank @Size(max = 8) @Pattern(regexp = ChartInterval.VALIDATION_PATTERN, message = "interval is invalid")
            @DefaultValue("1S") @QueryParam("interval") String interval,
            @Min(value = 1, message = "limit must be at least 1")
            @Max(value = MarketBarProvider.MAX_BARS, message = "limit must not exceed 2000")
            @DefaultValue("500") @QueryParam("limit") int limit,
            @Pattern(regexp = CurrencyCode.VALIDATION_PATTERN, message = "currency is invalid")
            @DefaultValue("USD") @QueryParam("currency") String currency) {
        return marketDataViewService.getBars(symbol, interval, limit, currency);
    }

    @GET
    @Path("/symbols")
    public List<String> getSymbols() {
        return marketSymbolProvider.getSupportedSymbols("USD");
    }

    @GET
    @Path("/currencies")
    public List<String> getSupportedCurrencies() {
        return currencyConversionService.getSupportedCurrencies();
    }

    @GET
    @Path("/signals")
    public List<AiSignal> getSignals(
            @NotBlank @Pattern(regexp = MarketSymbolNormalizer.VALIDATION_PATTERN, message = "symbol is invalid")
            @DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol,
            @Min(value = 1, message = "limit must be at least 1")
            @Max(value = 1_000, message = "limit must not exceed 1000")
            @DefaultValue("200") @QueryParam("limit") int limit) {
        return marketSignalProvider.getSignals(symbol, limit);
    }

    @GET
    @Path("/context")
    public MarketContextSnapshot getMarketContext(
        @NotBlank @Pattern(regexp = MarketSymbolNormalizer.VALIDATION_PATTERN, message = "symbol is invalid")
        @DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol
    ) {
        return marketContextService.get(symbol);
    }

    @GET
    @Path("/forecast")
    public MarketForecast getForecast(
            @NotBlank @Pattern(regexp = MarketSymbolNormalizer.VALIDATION_PATTERN, message = "symbol is invalid")
            @DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol,
            @Min(value = 1, message = "horizonDays must be at least 1")
            @Max(value = 365, message = "horizonDays must not exceed 365")
            @DefaultValue("1") @QueryParam("horizonDays") int horizonDays) {
        return marketForecastProvider.getForecast(symbol, horizonDays);
    }

    @GET
    @Path("/order-book")
    public OrderBookSnapshot getOrderBook(
            @NotBlank @Pattern(regexp = MarketSymbolNormalizer.VALIDATION_PATTERN, message = "symbol is invalid")
            @DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol,
            @Min(value = 1, message = "levels must be at least 1")
            @Max(value = 200, message = "levels must not exceed 200")
            @DefaultValue("12") @QueryParam("levels") int levels,
            @Pattern(regexp = CurrencyCode.VALIDATION_PATTERN, message = "currency is invalid")
            @DefaultValue("USD") @QueryParam("currency") String currency) {
        return marketDataViewService.getOrderBook(symbol, levels, currency);
    }

    @POST
    @Path("/context")
    public MarketContextSnapshot updateMarketContext(
            @NotBlank @Pattern(regexp = MarketSymbolNormalizer.VALIDATION_PATTERN, message = "symbol is invalid")
            @DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol,
            @NotNull(message = "market context payload is required") @Valid MarketContextUpdateRequest request) {
        marketContextService.update(symbol, request);
        return marketContextService.get(symbol);
    }
}
