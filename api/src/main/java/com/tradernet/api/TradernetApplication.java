package com.tradernet.api;

import com.tradernet.api.resources.AuthResource;
import com.tradernet.api.resources.GroupResource;
import com.tradernet.api.resources.HealthResource;
import com.tradernet.api.resources.OrderResource;
import com.tradernet.api.resources.PasswordResource;
import com.tradernet.api.resources.RoleResource;
import com.tradernet.api.resources.SignalResource;
import com.tradernet.api.resources.TradeResource;
import com.tradernet.api.resources.UserPropertyResource;
import com.tradernet.api.resources.UserResource;

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
            GroupResource.class,
            HealthResource.class,
            OrderResource.class,
            PasswordResource.class,
            RoleResource.class,
            SignalResource.class,
            TradeResource.class,
            UserPropertyResource.class,
            UserResource.class
        );
    }
}
