package com.tradernet.api;

import com.tradernet.api.resources.AuthResource;
import com.tradernet.api.resources.AuthenticationFilter;
import com.tradernet.api.resources.GroupResource;
import com.tradernet.api.resources.HealthResource;
import com.tradernet.api.resources.ConstraintViolationExceptionMapper;
import com.tradernet.api.resources.InvalidAccessControlAssignmentExceptionMapper;
import com.tradernet.api.resources.InvalidMarketContextExceptionMapper;
import com.tradernet.api.resources.MarketResource;
import com.tradernet.api.resources.NotAuthenticatedExceptionMapper;
import com.tradernet.api.resources.OrderResource;
import com.tradernet.api.resources.PortfolioResource;
import com.tradernet.api.resources.RoleResource;
import com.tradernet.api.resources.TradeResource;
import com.tradernet.api.resources.UserResource;
import com.tradernet.api.resources.WebApplicationExceptionMapper;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Set;

/**
 * Registers the JAX-RS application for Tradernet.
 */
@ApplicationPath("/")
public class TradernetApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
            AuthResource.class,
            AuthenticationFilter.class,
            ConstraintViolationExceptionMapper.class,
            GroupResource.class,
            HealthResource.class,
            InvalidAccessControlAssignmentExceptionMapper.class,
            InvalidMarketContextExceptionMapper.class,
            MarketResource.class,
            NotAuthenticatedExceptionMapper.class,
            OrderResource.class,
            PortfolioResource.class,
            RoleResource.class,
            TradeResource.class,
            UserResource.class,
            WebApplicationExceptionMapper.class
        );
    }
}
