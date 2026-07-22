package com.tradernet.user;

import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves protected API resources to their allowed roles.
 */
@Stateless
public class AuthorizationService implements AuthorizationOperations {

    @EJB
    private ResourceDao resourceDao;

    public AuthorizationService() {
    }

    AuthorizationService(ResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    public Set<String> getRequiredRoles(String httpMethod, String path) {
        final String normalisedPath = ResourcePathNormalizer.normalize(path);
        final Set<String> candidatePaths = candidatePathPrefixes(normalisedPath);
        if (candidatePaths.isEmpty()) {
            return Set.of();
        }

        final List<ResourceAccessRule> matchingRules = resourceDao
            .findMatchingWithRoles(candidatePaths, ResourceAccessRule.normalizeMethod(httpMethod))
            .stream()
            .map(ResourceAccessRule::fromResource)
            .filter(rule -> rule.matches(httpMethod, normalisedPath))
            .collect(Collectors.toList());
        if (matchingRules.isEmpty()) {
            return Set.of();
        }

        final int longestPath = matchingRules.stream()
            .map(rule -> rule.pathPrefix)
            .map(String::length)
            .max(Comparator.naturalOrder())
            .orElse(0);
        final int strongestMethodMatch = matchingRules.stream()
            .filter(rule -> rule.pathPrefix.length() == longestPath)
            .mapToInt(ResourceAccessRule::methodSpecificity)
            .max()
            .orElse(0);

        return matchingRules.stream()
            .filter(rule -> rule.pathPrefix.length() == longestPath)
            .filter(rule -> rule.methodSpecificity() == strongestMethodMatch)
            .flatMap(rule -> rule.allowedRoles.stream())
            .collect(Collectors.toSet());
    }

    private Set<String> candidatePathPrefixes(String path) {
        if (path == null || path.isBlank()) {
            return Set.of();
        }

        final Set<String> prefixes = new LinkedHashSet<>();
        String candidate = path;
        while (!candidate.isBlank()) {
            prefixes.add(candidate);
            final int separator = candidate.lastIndexOf('/');
            if (separator < 0) {
                break;
            }
            candidate = candidate.substring(0, separator);
        }
        return prefixes;
    }

    public boolean hasAnyRole(AuthUserDto authUser, Set<String> allowedRoles) {
        return authUser.getRoleNames() != null && authUser.getRoleNames().stream().anyMatch(allowedRoles::contains);
    }

    public boolean canReadOwnUserByUsername(String httpMethod, String path, AuthUserDto authUser) {
        if (!"GET".equalsIgnoreCase(httpMethod)) {
            return false;
        }

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

    private static class ResourceAccessRule {
        private final String pathPrefix;
        private final String httpMethod;
        private final Set<String> allowedRoles;

        private ResourceAccessRule(String pathPrefix, String httpMethod, Set<String> allowedRoles) {
            this.pathPrefix = pathPrefix;
            this.httpMethod = httpMethod;
            this.allowedRoles = allowedRoles;
        }

        private static ResourceAccessRule fromResource(ResourceEntity resource) {
            return new ResourceAccessRule(
                ResourcePathNormalizer.normalize(resource.getPathPrefix()),
                normalizeMethod(resource.getHttpMethod()),
                resource.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet())
            );
        }

        private boolean matches(String requestedMethod, String path) {
            final boolean methodMatches = httpMethod == null || httpMethod.equals(normalizeMethod(requestedMethod));
            return methodMatches
                && !pathPrefix.isBlank()
                && (path.equals(pathPrefix) || path.startsWith(pathPrefix + "/"));
        }

        private int methodSpecificity() {
            return httpMethod == null ? 0 : 1;
        }

        private static String normalizeMethod(String method) {
            return method == null || method.isBlank() || "*".equals(method.trim())
                ? null
                : method.trim().toUpperCase(Locale.ROOT);
        }
    }
}
