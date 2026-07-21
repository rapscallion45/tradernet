package com.tradernet.api.resources;

import com.tradernet.user.InvalidAccessControlAssignmentException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps invalid group and role assignments to the standard HTTP 400 response.
 */
@Provider
public class InvalidAccessControlAssignmentExceptionMapper
    implements ExceptionMapper<InvalidAccessControlAssignmentException> {

    @Override
    public Response toResponse(InvalidAccessControlAssignmentException exception) {
        return ApiErrors.response(Response.Status.BAD_REQUEST, exception.getMessage());
    }
}
