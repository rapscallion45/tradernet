package com.tradernet.api.resources;

import jakarta.ws.rs.core.NewCookie;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthResourceCookieTest {

    @Test
    void sessionCookieIsSecureHttpOnlyStrictAndNonPersistent() {
        final NewCookie cookie = AuthResource.sessionCookie("token", true);

        assertTrue(cookie.isSecure());
        assertTrue(cookie.isHttpOnly());
        assertEquals(NewCookie.SameSite.STRICT, cookie.getSameSite());
        assertEquals(NewCookie.DEFAULT_MAX_AGE, cookie.getMaxAge());
        assertEquals("/", cookie.getPath());
    }

    @Test
    void clearedCookiesKeepTheSameSecurityAttributes() {
        final NewCookie cookie = AuthResource.clearSessionCookie(true);

        assertTrue(cookie.isSecure());
        assertTrue(cookie.isHttpOnly());
        assertEquals(NewCookie.SameSite.STRICT, cookie.getSameSite());
        assertEquals(0, cookie.getMaxAge());
    }
}
