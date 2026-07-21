package com.tradernet.api;

import com.tradernet.api.resources.ConstraintViolationExceptionMapper;
import com.tradernet.api.resources.InvalidAccessControlAssignmentExceptionMapper;
import com.tradernet.api.resources.InvalidMarketContextExceptionMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TradernetApplicationTest {

    @Test
    void registersRequestValidationMappers() {
        TradernetApplication application = new TradernetApplication();

        assertTrue(application.getClasses().contains(ConstraintViolationExceptionMapper.class));
        assertTrue(application.getClasses().contains(InvalidAccessControlAssignmentExceptionMapper.class));
        assertTrue(application.getClasses().contains(InvalidMarketContextExceptionMapper.class));
    }
}
