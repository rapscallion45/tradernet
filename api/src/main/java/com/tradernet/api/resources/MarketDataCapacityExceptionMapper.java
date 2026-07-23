package com.tradernet.api.resources;

import com.tradernet.marketai.MarketDataCapacityException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps exhausted market client pools to a retryable service response.
 */
@Provider
public class MarketDataCapacityExceptionMapper implements ExceptionMapper<MarketDataCapacityException> {

    @Override
    public Response toResponse(MarketDataCapacityException exception) {
        return ApiErrors.response(Response.Status.SERVICE_UNAVAILABLE, exception.getMessage());
    }
}
