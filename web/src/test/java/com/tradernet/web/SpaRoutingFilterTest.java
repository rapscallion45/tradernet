package com.tradernet.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaRoutingFilterTest {

    @Test
    void forwardsExtensionlessBrowserRoutes() {
        assertTrue(SpaRoutingFilter.shouldForward("GET", "/charts", false));
        assertTrue(SpaRoutingFilter.shouldForward("HEAD", "/admin/users", false));
    }

    @Test
    void leavesServerAndStaticResourceRequestsToTheContainer() {
        assertFalse(SpaRoutingFilter.shouldForward("GET", "/api/market/bars", false));
        assertFalse(SpaRoutingFilter.shouldForward("GET", "/assets/missing", false));
        assertFalse(SpaRoutingFilter.shouldForward("GET", "/assets/index.js", false));
        assertFalse(SpaRoutingFilter.shouldForward("GET", "/favicon.ico", false));
        assertFalse(SpaRoutingFilter.shouldForward("GET", "/WEB-INF/classes", false));
        assertFalse(SpaRoutingFilter.shouldForward("GET", "/META-INF/resources", false));
        assertFalse(SpaRoutingFilter.shouldForward("GET", "/", false));
        assertFalse(SpaRoutingFilter.shouldForward("GET", "/index.html", true));
    }

    @Test
    void doesNotForwardMutationRequests() {
        assertFalse(SpaRoutingFilter.shouldForward("POST", "/charts", false));
    }
}
