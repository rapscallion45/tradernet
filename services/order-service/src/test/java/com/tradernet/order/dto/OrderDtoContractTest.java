package com.tradernet.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderDtoContractTest {

    @Test
    void responseUsesIdAsItsOnlyOrderIdentifier() {
        boolean hasLegacyOrderIdAccessor = Arrays.stream(OrderResponseDto.class.getMethods())
            .map(Method::getName)
            .anyMatch(name -> name.equals("getOrderId") || name.equals("setOrderId"));

        assertFalse(hasLegacyOrderIdAccessor);
    }

    @Test
    void requestFieldsCarryBeanValidationConstraints() throws NoSuchFieldException {
        Field symbol = OrderRequestDto.class.getDeclaredField("symbol");
        Field side = OrderRequestDto.class.getDeclaredField("side");
        Field quantity = OrderRequestDto.class.getDeclaredField("quantity");
        Field price = OrderRequestDto.class.getDeclaredField("price");

        assertNotNull(symbol.getAnnotation(NotBlank.class));
        assertNotNull(side.getAnnotation(NotNull.class));
        assertNotNull(quantity.getAnnotation(NotNull.class));
        assertNotNull(quantity.getAnnotation(Positive.class));
        assertNotNull(price.getAnnotation(NotNull.class));
        assertNotNull(price.getAnnotation(Positive.class));
    }
}
