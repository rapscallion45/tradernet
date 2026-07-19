package com.tradernet.user;

import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves protected API resources to their allowed roles.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class AuthorizationService {

    @EJB
    private ResourceDao resourceDao;

    private volatile List<ResourceAccessRule> cachedRules;

    public Set<String> getRequiredRoles(String path) {
        final String normalisedPath = ResourcePathNormalizer.normalize(path);
        return rules().stream()
            .filter(rule -> rule.matches(normalisedPath))
            .flatMap(rule -> rule.allowedRoles.stream())
            .collect(Collectors.toSet());
    }

    public boolean hasAnyRole(AuthUserDto authUser, Set<String> allowedRoles) {
        return authUser.getRoleNames() != null && authUser.getRoleNames().stream().anyMatch(allowedRoles::contains);
    }

    public boolean canReadOwnUserByUsername(String path, AuthUserDto authUser) {
        final String normalisedPath = ResourcePathNormalizer.normalize(path);
        final String byUsernamePrefix = "users/by-username/";
        if (!normalisedPath.startsWith(byUsernamePrefix)) {
            return false;
        }

        final String requestedUsername = normalisedPath.substring(byUsernamePrefix.length());
        if (requestedUsername.isBlank()) {
            return false;
        }

        final String currentUsername = authUser.getUsername();
        return currentUsername != null && currentUsername.equalsIgnoreCase(requestedUsername);
    }

    public void invalidate() {
        cachedRules = null;
    }

    private List<ResourceAccessRule> rules() {
        List<ResourceAccessRule> snapshot = cachedRules;
        if (snapshot != null) {
            return snapshot;
        }

        synchronized (this) {
            snapshot = cachedRules;
            if (snapshot == null) {
                snapshot = resourceDao.findAllWithRoles().stream()
                    .map(ResourceAccessRule::fromResource)
                    .collect(Collectors.toList());
                cachedRules = snapshot;
            }
            return snapshot;
        }
    }

    private static class ResourceAccessRule {
        private final String pathPrefix;
        private final Set<String> allowedRoles;

        private ResourceAccessRule(String pathPrefix, Set<String> allowedRoles) {
            this.pathPrefix = pathPrefix;
            this.allowedRoles = allowedRoles;
        }

        private static ResourceAccessRule fromResource(ResourceEntity resource) {
            return new ResourceAccessRule(
                ResourcePathNormalizer.normalize(resource.getPathPrefix()),
                resource.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet())
            );
        }

        private boolean matches(String path) {
            return !pathPrefix.isBlank() && (path.equals(pathPrefix) || path.startsWith(pathPrefix + "/"));
        }
    }
}
