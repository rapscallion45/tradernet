package com.tradernet.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.order.dto.OrderResponseDto;
import com.tradernet.trade.dto.TradeResponseDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiObjectMapperProviderTest {

    @Test
    void serializesJavaTimeValuesAsIso8601Strings() {
        final ObjectMapper objectMapper = ApiObjectMapperProvider.createObjectMapper();
        final OrderResponseDto order = new OrderResponseDto();
        order.setCreatedAt(Instant.parse("2026-07-22T12:34:10Z"));
        final TradeResponseDto trade = new TradeResponseDto();
        trade.setTimestamp(LocalDateTime.parse("2026-07-22T12:34:10"));

        final JsonNode orderJson = objectMapper.valueToTree(order);
        final JsonNode tradeJson = objectMapper.valueToTree(trade);

        assertEquals("2026-07-22T12:34:10Z", orderJson.get("createdAt").asText());
        assertEquals("2026-07-22T12:34:10", tradeJson.get("timestamp").asText());
    }
}
