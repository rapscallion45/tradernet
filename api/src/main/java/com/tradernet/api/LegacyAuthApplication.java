package com.tradernet.api;

import com.tradernet.api.resources.AuthResource;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Set;

/**
 * Provides a legacy auth endpoint without the /api prefix for compatibility.
 */
@ApplicationPath("/")
public class LegacyAuthApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(AuthResource.class);
    }
}
