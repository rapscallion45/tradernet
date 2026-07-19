package com.tradernet.api.resources;

import jakarta.ws.rs.core.Response;

/**
 * Small factory for consistent HTTP error responses.
 */
final class ApiErrors {

    private ApiErrors() {
    }

    static Response response(Response.Status status, String message) {
        return status(status, message).build();
    }

    static Response.ResponseBuilder status(Response.Status status, String message) {
        return Response.status(status).entity(dto(status.getStatusCode(), status.getReasonPhrase(), message));
    }

    static ApiErrorDto dto(Response.StatusType statusType, String message) {
        return dto(statusType.getStatusCode(), statusType.getReasonPhrase(), message);
    }

    static ApiErrorDto dto(int status, String code, String message) {
        final String resolvedMessage = message == null || message.isBlank() ? code : message;
        return new ApiErrorDto(status, code, resolvedMessage);
    }
}
