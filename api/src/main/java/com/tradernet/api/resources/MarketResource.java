package com.tradernet.api.resources;

import com.tradernet.currencyconversion.CurrencyCode;
import com.tradernet.currencyconversion.CurrencyConversionService;
import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.MarketBar;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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
        return marketAiService.getBars(symbol, interval, limit).stream()
                .map(bar -> currencyConversionService.convertBar(bar, targetCurrency))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/symbols")
    public List<String> getSymbols(@DefaultValue("USD") @QueryParam("currency") String currency) {
        return marketAiService.getSupportedSymbols(currency);
    }

    @GET
    @Path("/signals")
    public List<AiSignal> getSignals(@DefaultValue("200") @QueryParam("limit") int limit) {
        return marketAiService.getSignals(limit);
    }
}
