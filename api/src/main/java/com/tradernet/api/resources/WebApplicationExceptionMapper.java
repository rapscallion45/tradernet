package com.tradernet.api.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Converts standard JAX-RS HTTP exceptions into the Tradernet API error shape.
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        final Response response = exception.getResponse();
        final Response.StatusType statusInfo = response == null
            ? Response.Status.INTERNAL_SERVER_ERROR
            : response.getStatusInfo();
        final Response.ResponseBuilder builder = response == null
            ? Response.status(statusInfo)
            : Response.fromResponse(response);
        return builder
            .entity(ApiErrors.dto(statusInfo, message(exception, statusInfo)))
            .build();
    }

    private String message(WebApplicationException exception, Response.StatusType statusInfo) {
        final String message = exception.getMessage();
        if (message == null || message.isBlank() || message.startsWith("HTTP ")) {
            return statusInfo.getReasonPhrase();
        }
        return message;
    }
}
