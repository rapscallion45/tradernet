package com.tradernet.user;

import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.user.dto.AuthUserDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationServiceTest {

    @Test
    void separatesReadAndWriteRolesForTheSameResourcePath() {
        final ResourceEntity marketRead = resource("Market", "market", "GET", "Standard Rights");
        final ResourceEntity contextWrite = resource(
            "Market Context Administration",
            "market/context",
            "POST",
            "Admin Rights"
        );
        final AuthorizationService service = new AuthorizationService(resourceDao(List.of(marketRead, contextWrite)));

        assertEquals(Set.of("Standard Rights"), service.getRequiredRoles("GET", "market/context"));
        assertEquals(Set.of("Admin Rights"), service.getRequiredRoles("POST", "market/context"));
        assertEquals(Set.of(), service.getRequiredRoles("DELETE", "market/context"));

        final AuthUserDto user = new AuthUserDto(1L, "alice", Set.of("Standard Rights"));
        assertTrue(service.canReadOwnUserByUsername("GET", "users/by-username/alice", user));
        assertFalse(service.canReadOwnUserByUsername("PUT", "users/by-username/alice", user));
    }

    @Test
    void selectsTheMostSpecificPathAndMethodPolicy() {
        final ResourceEntity marketRead = resource("Market", "market", "GET", "Standard Rights");
        final ResourceEntity adminWildcard = resource("Admin", "market/admin", null, "Operations Rights");
        final ResourceEntity adminRead = resource("Admin Read", "market/admin", "GET", "Admin Rights");
        final AuthorizationService service = new AuthorizationService(
            resourceDao(List.of(marketRead, adminWildcard, adminRead))
        );

        assertEquals(Set.of("Admin Rights"), service.getRequiredRoles("GET", "market/admin/audit"));
        assertEquals(Set.of("Operations Rights"), service.getRequiredRoles("POST", "market/admin/audit"));
    }

    @Test
    void requestsOnlyTheExactPathAndItsParentPrefixes() {
        final ResourceEntity marketRead = resource("Market", "market", "GET", "Standard Rights");
        final ResourceEntity adminRead = resource("Admin Read", "market/admin", "GET", "Admin Rights");
        final ResourceEntity unrelated = resource("Orders", "orders", "GET", "Order Rights");
        final RecordingResourceDao resourceDao = new RecordingResourceDao(List.of(marketRead, adminRead, unrelated));
        final AuthorizationService service = new AuthorizationService(resourceDao);

        assertEquals(Set.of("Admin Rights"), service.getRequiredRoles("GET", "/market/admin/audit/"));
        assertEquals(Set.of("market/admin/audit", "market/admin", "market"), resourceDao.requestedPaths);
        assertEquals("GET", resourceDao.requestedMethod);
    }

    private ResourceEntity resource(String name, String path, String method, String roleName) {
        final ResourceEntity resource = new ResourceEntity();
        resource.setName(name);
        resource.setPathPrefix(path);
        resource.setHttpMethod(method);
        final RoleEntity role = new RoleEntity();
        role.setName(roleName);
        role.addResource(resource);
        return resource;
    }

    private ResourceDao resourceDao(List<ResourceEntity> resources) {
        return new RecordingResourceDao(resources);
    }

    private static final class RecordingResourceDao implements ResourceDao {
        private final List<ResourceEntity> resources;
        private Set<String> requestedPaths = Set.of();
        private String requestedMethod;

        private RecordingResourceDao(List<ResourceEntity> resources) {
            this.resources = resources;
        }

        @Override
        public ResourceEntity save(ResourceEntity resource) {
            return resource;
        }

        @Override
        public List<ResourceEntity> findAll() {
            return resources;
        }

        @Override
        public List<ResourceEntity> findMatchingWithRoles(Set<String> pathPrefixes, String httpMethod) {
            requestedPaths = Set.copyOf(pathPrefixes);
            requestedMethod = httpMethod;
            return resources.stream()
                .filter(resource -> pathPrefixes.contains(resource.getPathPrefix()))
                .filter(resource -> resource.getHttpMethod() == null
                    || resource.getHttpMethod().isBlank()
                    || "*".equals(resource.getHttpMethod())
                    || resource.getHttpMethod().trim().toUpperCase(Locale.ROOT).equals(httpMethod))
                .collect(Collectors.toList());
        }

        @Override
        public Optional<ResourceEntity> findByName(String name) {
            return Optional.empty();
        }

        @Override
        public List<ResourceEntity> findByNames(Set<String> names) {
            return List.of();
        }

        @Override
        public Optional<ResourceEntity> findByPathPrefix(String pathPrefix) {
            return Optional.empty();
        }
    }
}
