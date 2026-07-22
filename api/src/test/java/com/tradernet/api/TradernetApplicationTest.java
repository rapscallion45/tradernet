package com.tradernet.api;

import com.tradernet.api.resources.ConstraintViolationExceptionMapper;
import com.tradernet.api.resources.InvalidAccessControlAssignmentExceptionMapper;
import com.tradernet.api.resources.InvalidMarketContextExceptionMapper;
import com.tradernet.api.resources.MarketDataCapacityExceptionMapper;
import com.tradernet.api.resources.InvalidPasswordExceptionMapper;
import com.tradernet.api.resources.AuthenticationResponseFilter;
import com.tradernet.api.resources.UnhandledExceptionMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TradernetApplicationTest {

    @Test
    void registersRequestValidationMappers() {
        TradernetApplication application = new TradernetApplication();

        assertTrue(application.getClasses().contains(ApiObjectMapperProvider.class));
        assertTrue(application.getClasses().contains(ConstraintViolationExceptionMapper.class));
        assertTrue(application.getClasses().contains(InvalidAccessControlAssignmentExceptionMapper.class));
        assertTrue(application.getClasses().contains(InvalidMarketContextExceptionMapper.class));
        assertTrue(application.getClasses().contains(MarketDataCapacityExceptionMapper.class));
        assertTrue(application.getClasses().contains(InvalidPasswordExceptionMapper.class));
        assertTrue(application.getClasses().contains(AuthenticationResponseFilter.class));
        assertTrue(application.getClasses().contains(UnhandledExceptionMapper.class));
    }
}
