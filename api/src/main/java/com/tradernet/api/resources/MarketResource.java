package com.tradernet.api.resources;

import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.currencyconversion.CurrencyConversionService;
import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.marketai.model.MarketContextSnapshot;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for chart bars and generated AI signals.
 */
@Path("/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MarketResource {

    @Inject
    private MarketAiService marketAiService;

    @Inject
    private CurrencyConversionService currencyConversionService;

    @GET
    @Path("/bars")
    public List<MarketBar> getBars(
            @DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol,
            @DefaultValue("1S") @QueryParam("interval") String interval,
            @DefaultValue("500") @QueryParam("limit") int limit,
            @DefaultValue("USD") @QueryParam("currency") String currency) {
        CurrencyCode targetCurrency = CurrencyCode.parseOrDefault(currency, CurrencyCode.USD);
        List<MarketBar> rawBars = marketAiService.getBars(symbol, interval, limit);

        try {
            return rawBars.stream()
                    .map(bar -> currencyConversionService.convertBar(bar, targetCurrency))
                    .collect(Collectors.toList());
        } catch (RuntimeException ex) {
            return rawBars;
        }
    }

    @GET
    @Path("/symbols")
    public List<String> getSymbols() {
        return marketAiService.getSupportedSymbols("USD");
    }

    @GET
    @Path("/currencies")
    public List<String> getSupportedCurrencies() {
        return currencyConversionService.getSupportedCurrencies();
    }

    @GET
    @Path("/signals")
    public List<AiSignal> getSignals(
            @DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol,
            @DefaultValue("200") @QueryParam("limit") int limit) {
        return marketAiService.getSignals(symbol, limit);
    }

    @GET
    @Path("/context")
    public MarketContextSnapshot getMarketContext(@DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol) {
        return marketAiService.getMarketContext(symbol);
    }

    @POST
    @Path("/context")
    public MarketContextSnapshot updateMarketContext(
            @DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol,
            MarketContextSnapshot snapshot) {
        marketAiService.updateMarketContext(symbol, snapshot);
        return marketAiService.getMarketContext(symbol);
    }
}
