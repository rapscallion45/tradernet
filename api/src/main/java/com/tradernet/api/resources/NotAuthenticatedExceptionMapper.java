package com.tradernet.api.resources;

import com.tradernet.user.dto.MessageResponseDto;
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
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new MessageResponseDto(exception.getMessage()))
            .build();
    }
}
