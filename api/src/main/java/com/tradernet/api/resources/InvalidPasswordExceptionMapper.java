package com.tradernet.api.resources;

import com.tradernet.user.InvalidPasswordException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps password-policy violations to the standard HTTP 400 error contract.
 */
@Provider
public class InvalidPasswordExceptionMapper implements ExceptionMapper<InvalidPasswordException> {

    @Override
    public Response toResponse(InvalidPasswordException exception) {
        return ApiErrors.response(Response.Status.BAD_REQUEST, exception.getMessage());
    }
}
