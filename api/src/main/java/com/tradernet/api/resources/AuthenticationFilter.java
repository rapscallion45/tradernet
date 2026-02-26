package com.tradernet.api.resources;

import com.tradernet.user.dto.MessageResponseDto;
import com.tradernet.user.dto.AuthUserDto;
import com.tradernet.user.UserService;
import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.jpa.entities.RoleEntity;
import jakarta.inject.Inject;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enforces authenticated sessions for all non-auth REST endpoints.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    @Inject
    private UserService userService;

    @Inject
    private ResourceDao resourceDao;

    @Inject
    private RoleDao roleDao;

    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
        "auth",
        "auth/login",
        "auth/logout",
        "auth/session",
        "auth/forgot-password"
    );
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

    private boolean hasAnyRole(AuthUserDto authUser, Set<String> allowedRoles) {
        return authUser.getRoleNames() != null && authUser.getRoleNames().stream().anyMatch(allowedRoles::contains);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        ensureDefaultResourceRoleAssignments();

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

        AuthUserDto effectiveAuthUser = userService.findByUsernameWithRoles(authUser.get().getUsername())
            .map(AuthUserDto::fromUser)
            .orElse(authUser.get());

        Set<String> requiredRoles = resourceDao.findAllWithRoles().stream()
            .filter(resource -> pathMatchesResource(path, resource))
            .flatMap(resource -> resource.getRoles().stream())
            .map(role -> role.getName())
            .collect(Collectors.toSet());

        if (!requiredRoles.isEmpty() && !hasAnyRole(effectiveAuthUser, requiredRoles)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity(new MessageResponseDto("Insufficient permissions"))
                .build());
        }
    }

    private boolean pathMatchesResource(String path, ResourceEntity resource) {
        String normalisedPath = normalisePath(path);
        String pathPrefix = resource.getPathPrefix();
        return pathPrefix != null && !pathPrefix.isBlank() && normalisedPath.startsWith(pathPrefix);
    }

    private void ensureDefaultResourceRoleAssignments() {
        ensureResourceExists("Users", "users");
        ensureResourceExists("Groups", "groups");
        ensureResourceExists("Security Roles", "roles");

        List<RoleEntity> roles = roleDao.findAllWithResources();
        if (roles.isEmpty()) {
            return;
        }

        assignDefaultIfUnconfigured("Users", Set.of("SUPER USER", "ADMIN"), roles);
        assignDefaultIfUnconfigured("Groups", Set.of("SUPER USER", "ADMIN"), roles);
        assignDefaultIfUnconfigured("Security Roles", Set.of("SUPER USER"), roles);
    }

    private void ensureResourceExists(String resourceName, String pathPrefix) {
        if (resourceDao.findByName(resourceName).isPresent()) {
            return;
        }

        ResourceEntity resource = new ResourceEntity();
        resource.setName(resourceName);
        resource.setPathPrefix(pathPrefix);
        resourceDao.save(resource);
    }

    private void assignDefaultIfUnconfigured(String resourceName, Set<String> defaultRoleNames, List<RoleEntity> roles) {
        ResourceEntity resource = resourceDao.findByName(resourceName).orElse(null);
        if (resource == null || !resource.getRoles().isEmpty()) {
            return;
        }

        Set<RoleEntity> matchingRoles = roles.stream()
            .filter(role -> defaultRoleNames.contains(role.getName()))
            .collect(Collectors.toCollection(HashSet::new));

        if (matchingRoles.isEmpty()) {
            return;
        }

        for (RoleEntity role : matchingRoles) {
            role.addResource(resource);
            roleDao.save(role);
        }
    }
}
