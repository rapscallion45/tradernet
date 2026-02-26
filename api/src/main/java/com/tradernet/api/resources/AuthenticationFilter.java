package com.tradernet.api.resources;

import com.tradernet.user.dto.MessageResponseDto;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.Optional;
import java.util.Set;

/**
 * Enforces authenticated sessions for all non-auth REST endpoints.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
        "auth",
        "auth/login",
        "auth/logout",
        "auth/session",
        "auth/forgot-password"
    );
    private static final Set<String> ADMIN_ROLES = Set.of("SUPER USER", "ADMIN");
    private static final Set<String> SUPER_USER_ROLES = Set.of("SUPER USER");

    private String normalisePath(String path) {
        if (path == null) {
            return "";
        }

        String normalisedPath = path.startsWith("/") ? path.substring(1) : path;
        if (normalisedPath.endsWith("/")) {
            normalisedPath = normalisedPath.substring(0, normalisedPath.length() - 1);
        }

        return normalisedPath;
    }

    private boolean isPublicAuthPath(String path) {
        return PUBLIC_AUTH_PATHS.contains(normalisePath(path));
    }

    private boolean isUsersOrGroupsApiPath(String path) {
        String normalisedPath = normalisePath(path);
        return normalisedPath.startsWith("users") || normalisedPath.startsWith("groups");
    }

    private boolean isRolesApiPath(String path) {
        String normalisedPath = normalisePath(path);
        return normalisedPath.startsWith("roles");
    }

    private boolean hasAnyRole(AuthUserDto authUser, Set<String> allowedRoles) {
        return authUser.getRoleNames() != null && authUser.getRoleNames().stream().anyMatch(allowedRoles::contains);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        if (isPublicAuthPath(path)) {
            return;
        }

        Cookie sessionCookie = requestContext.getCookies().get(AuthResource.SESSION_COOKIE_NAME);
        String sessionId = sessionCookie == null ? null : sessionCookie.getValue();
        Optional<AuthUserDto> authUser = AuthResource.getSessionUser(sessionId);

        if (authUser.isEmpty()) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(new MessageResponseDto("Not authenticated"))
                .build());
            return;
        }

        if (isUsersOrGroupsApiPath(path) && !hasAnyRole(authUser.get(), ADMIN_ROLES)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity(new MessageResponseDto("Insufficient permissions"))
                .build());
            return;
        }

        if (isRolesApiPath(path) && !hasAnyRole(authUser.get(), SUPER_USER_ROLES)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity(new MessageResponseDto("Insufficient permissions"))
                .build());
        }
    }
}
