package com.tradernet.api.resources;

import com.tradernet.user.AuthorizationDeniedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps service-layer authorization denials to the standard API error contract.
 */
@Provider
public class AuthorizationDeniedExceptionMapper implements ExceptionMapper<AuthorizationDeniedException> {

    @Override
    public Response toResponse(AuthorizationDeniedException exception) {
        return ApiErrors.response(Response.Status.FORBIDDEN, exception.getMessage());
    }
}
