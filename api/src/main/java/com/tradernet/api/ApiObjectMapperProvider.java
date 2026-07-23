package com.tradernet.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * Defines the JSON contract shared by REST responses and websocket payloads.
 */
@Provider
public class ApiObjectMapperProvider implements ContextResolver<ObjectMapper> {

    private final ObjectMapper objectMapper = createObjectMapper();

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }

    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
