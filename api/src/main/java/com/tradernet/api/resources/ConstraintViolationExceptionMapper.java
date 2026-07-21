package com.tradernet.api.resources;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Comparator;

/**
 * Converts Bean Validation failures into the standard Tradernet API error body.
 */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        final String message = exception.getConstraintViolations().stream()
            .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
            .map(ConstraintViolation::getMessage)
            .findFirst()
            .orElse("Request validation failed");
        return ApiErrors.response(Response.Status.BAD_REQUEST, message);
    }
}
