package com.tradernet.api.resources;

import com.tradernet.currencyconversion.CurrencyConversionService;
import com.tradernet.marketai.MarketAiService;
import com.tradernet.marketai.MarketDataViewService;
import com.tradernet.marketai.forecast.MarketForecast;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.MarketBar;
import com.tradernet.marketai.model.MarketContextSnapshot;
import com.tradernet.marketai.orderbook.OrderBookSnapshot;
import jakarta.ejb.EJB;
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
    private MarketAiService marketAiService;

    @EJB
    private CurrencyConversionService currencyConversionService;

    @EJB
    private MarketDataViewService marketDataViewService;

    @GET
    @Path("/bars")
    public List<MarketBar> getBars(
            @DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol,
            @DefaultValue("1S") @QueryParam("interval") String interval,
            @DefaultValue("500") @QueryParam("limit") int limit,
            @DefaultValue("USD") @QueryParam("currency") String currency) {
        return marketDataViewService.getBars(symbol, interval, limit, currency);
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

    @GET
    @Path("/forecast")
    public MarketForecast getForecast(
            @DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol,
            @DefaultValue("1") @QueryParam("horizonDays") int horizonDays) {
        return marketAiService.getForecast(symbol, horizonDays);
    }

    @GET
    @Path("/order-book")
    public OrderBookSnapshot getOrderBook(
            @DefaultValue("BTCUSDT") @QueryParam("symbol") String symbol,
            @DefaultValue("12") @QueryParam("levels") int levels,
            @DefaultValue("USD") @QueryParam("currency") String currency) {
        return marketDataViewService.getOrderBook(symbol, levels, currency);
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
