package com.tradernet.api.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class WebApplicationExceptionMapperTest {

    @Test
    void preservesProtocolHeadersWhenStandardizingTheErrorBody() {
        final Response original = Response.status(Response.Status.METHOD_NOT_ALLOWED)
            .header(HttpHeaders.ALLOW, "GET")
            .header("Retry-After", "15")
            .build();

        final Response mapped = new WebApplicationExceptionMapper()
            .toResponse(new WebApplicationException("Unsupported method", original));

        assertEquals(Response.Status.METHOD_NOT_ALLOWED.getStatusCode(), mapped.getStatus());
        assertEquals("GET", mapped.getHeaderString(HttpHeaders.ALLOW));
        assertEquals("15", mapped.getHeaderString("Retry-After"));
        assertInstanceOf(ApiErrorDto.class, mapped.getEntity());
    }
}
