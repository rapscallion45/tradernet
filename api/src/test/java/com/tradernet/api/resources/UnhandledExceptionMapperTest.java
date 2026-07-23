package com.tradernet.api.resources;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UnhandledExceptionMapperTest {

    @Test
    void returnsSafeJsonErrorWithOperatorReference() {
        final Response response = new UnhandledExceptionMapper().toResponse(new IllegalStateException("sensitive"));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("application/json", response.getMediaType().toString());
        assertNotNull(response.getHeaderString(UnhandledExceptionMapper.ERROR_REFERENCE_HEADER));
        final ApiErrorDto body = assertInstanceOf(ApiErrorDto.class, response.getEntity());
        assertEquals("An unexpected server error occurred", body.getError().getErrorMessage());
        assertNotNull(body.getError().getReferenceId());
    }
}
