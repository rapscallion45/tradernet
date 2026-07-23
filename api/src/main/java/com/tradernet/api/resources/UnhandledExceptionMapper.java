package com.tradernet.api.resources;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Converts unexpected application failures into a safe, traceable JSON response.
 */
@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Exception> {

    public static final String ERROR_REFERENCE_HEADER = "X-Error-Reference";
    private static final Logger LOG = LoggerFactory.getLogger(UnhandledExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        final String referenceId = UUID.randomUUID().toString();
        LOG.error("Unhandled API failure [{}].", referenceId, exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .header(ERROR_REFERENCE_HEADER, referenceId)
            .entity(ApiErrors.dto(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected server error occurred",
                referenceId
            ))
            .build();
    }
}
