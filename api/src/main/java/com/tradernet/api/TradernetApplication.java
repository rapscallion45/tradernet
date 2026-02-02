package com.tradernet.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Registers the JAX-RS application for Tradernet.
 */
@ApplicationPath("/api")
public class TradernetApplication extends Application {
}
