package com.tradernet.api.resources;

import com.tradernet.currencyconversion.CurrencyConversionUnavailableException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps unavailable provider-backed FX data to a retryable response.
 */
@Provider
public class CurrencyConversionUnavailableExceptionMapper
    implements ExceptionMapper<CurrencyConversionUnavailableException> {

    @Override
    public Response toResponse(CurrencyConversionUnavailableException exception) {
        return ApiErrors.response(Response.Status.SERVICE_UNAVAILABLE, exception.getMessage());
    }
}
