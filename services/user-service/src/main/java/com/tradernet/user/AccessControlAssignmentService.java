package com.tradernet.user;

import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.dao.UserDao;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.jpa.entities.UserEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves validated access-control assignment names in bounded batch queries.
 */
@Stateless
public class AccessControlAssignmentService {

    @EJB
    private UserDao userDao;

    @EJB
    private RoleDao roleDao;

    @EJB
    private ResourceDao resourceDao;

    public Set<UserEntity> resolveUsers(Set<String> usernames) {
        final Set<String> requested = normalizeNames(usernames, "Username", true);
        if (requested.isEmpty()) {
            return new HashSet<>();
        }
        return resolve(
            requested,
            userDao.findByUsernames(requested),
            user -> user.getUsername().toLowerCase(Locale.ROOT),
            "User not found: "
        );
    }

    public Set<RoleEntity> resolveRoles(Set<String> roleNames) {
        final Set<String> requested = normalizeNames(roleNames, "Role name", false);
        if (requested.isEmpty()) {
            return new HashSet<>();
        }
        return resolve(requested, roleDao.findByNames(requested), RoleEntity::getName, "Role not found: ");
    }

    public Set<ResourceEntity> resolveResources(Set<String> resourceNames) {
        final Set<String> requested = normalizeNames(resourceNames, "Resource name", false);
        if (requested.isEmpty()) {
            return new HashSet<>();
        }
        return resolve(
            requested,
            resourceDao.findByNames(requested),
            ResourceEntity::getName,
            "Resource not found: "
        );
    }

    private Set<String> normalizeNames(Set<String> names, String label, boolean lowerCase) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }

        final Set<String> normalized = new LinkedHashSet<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                throw new InvalidAccessControlAssignmentException(label + " is required");
            }
            final String trimmed = name.trim();
            normalized.add(lowerCase ? trimmed.toLowerCase(Locale.ROOT) : trimmed);
        }
        return normalized;
    }

    private <T> Set<T> resolve(
        Set<String> requestedNames,
        List<T> resolvedValues,
        Function<T, String> nameExtractor,
        String missingMessagePrefix
    ) {
        final Map<String, T> valuesByName = resolvedValues.stream()
            .collect(Collectors.toMap(nameExtractor, Function.identity()));
        for (String requestedName : requestedNames) {
            if (!valuesByName.containsKey(requestedName)) {
                throw new InvalidAccessControlAssignmentException(missingMessagePrefix + requestedName);
            }
        }
        return new HashSet<>(valuesByName.values());
    }
}
