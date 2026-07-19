package com.tradernet.api.resources;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Converts missing-authentication failures into a standard API response.
 */
@Provider
public class NotAuthenticatedExceptionMapper implements ExceptionMapper<NotAuthenticatedException> {

    @Override
    public Response toResponse(NotAuthenticatedException exception) {
        return ApiErrors.response(Response.Status.UNAUTHORIZED, exception.getMessage());
    }
}
