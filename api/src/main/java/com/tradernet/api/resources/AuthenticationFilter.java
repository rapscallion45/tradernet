package com.tradernet.api.resources;

import com.tradernet.user.dto.MessageResponseDto;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Enforces authenticated sessions for all non-auth REST endpoints.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final String AUTH_BASE_PATH = "auth";
    private static final String AUTH_BASE_PATH_WITH_SLASH = "/auth";

    private boolean isPublicAuthPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        String normalisedPath = path.startsWith("/") ? path : "/" + path;
        return normalisedPath.equals(AUTH_BASE_PATH_WITH_SLASH + "/login")
            || normalisedPath.equals(AUTH_BASE_PATH_WITH_SLASH + "/logout")
            || normalisedPath.equals(AUTH_BASE_PATH_WITH_SLASH + "/session")
            || normalisedPath.equals(AUTH_BASE_PATH_WITH_SLASH + "/forgot-password")
            || normalisedPath.startsWith(AUTH_BASE_PATH_WITH_SLASH + "/")
            || normalisedPath.equals(AUTH_BASE_PATH_WITH_SLASH)
            || path.startsWith(AUTH_BASE_PATH);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        if (isPublicAuthPath(path)) {
            return;
        }

        Cookie sessionCookie = requestContext.getCookies().get(AuthResource.SESSION_COOKIE_NAME);
        String sessionId = sessionCookie == null ? null : sessionCookie.getValue();

        if (AuthResource.getSessionUser(sessionId).isPresent()) {
            return;
        }

        requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
            .entity(new MessageResponseDto("Not authenticated"))
            .build());
    }
}
