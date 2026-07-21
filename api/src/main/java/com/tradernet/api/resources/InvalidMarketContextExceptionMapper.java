package com.tradernet.api.resources;

import com.tradernet.marketai.context.InvalidMarketContextException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps market-context request validation failures to HTTP 400.
 */
@Provider
public class InvalidMarketContextExceptionMapper implements ExceptionMapper<InvalidMarketContextException> {

    @Override
    public Response toResponse(InvalidMarketContextException exception) {
        return ApiErrors.response(Response.Status.BAD_REQUEST, exception.getMessage());
    }
}
