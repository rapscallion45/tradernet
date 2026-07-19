package com.tradernet.api.resources;

import com.tradernet.user.dto.MessageResponseDto;
import com.tradernet.user.dto.AuthUserDto;
import com.tradernet.user.AuthSessionService;
import com.tradernet.user.AuthorizationService;
import com.tradernet.user.ResourcePathNormalizer;
import jakarta.annotation.Priority;
import jakarta.ejb.EJB;
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

    @EJB
    private AuthSessionService authSessionService;

    @EJB
    private AuthorizationService authorizationService;

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "auth",
        "auth/login",
        "auth/logout",
        "auth/session",
        "auth/forgot-password",
        "health"
    );

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.contains(ResourcePathNormalizer.normalize(path));
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        if (isPublicPath(path)) {
            return;
        }

        Cookie sessionCookie = requestContext.getCookies().get(AuthResource.SESSION_COOKIE_NAME);
        String sessionId = sessionCookie == null ? null : sessionCookie.getValue();
        Optional<AuthUserDto> authUser = authSessionService.getSessionUser(sessionId);

        if (authUser.isEmpty()) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(new MessageResponseDto("Not authenticated"))
                .build());
            return;
        }

        AuthUserDto effectiveAuthUser = authUser.get();
        AuthenticatedRequest.setAuthenticatedUser(requestContext, effectiveAuthUser);

        Set<String> requiredRoles = authorizationService.getRequiredRoles(path);

        if (authorizationService.canReadOwnUserByUsername(path, effectiveAuthUser)) {
            return;
        }

        if (requiredRoles.isEmpty()) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity(new MessageResponseDto("No permissions configured for this resource"))
                .build());
            return;
        }

        if (!authorizationService.hasAnyRole(effectiveAuthUser, requiredRoles)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity(new MessageResponseDto("Insufficient permissions"))
                .build());
        }
    }
}
