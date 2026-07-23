package com.tradernet.user;

import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.user.dto.AuthUserDto;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleManagementServiceAuthorizationTest {

    @Test
    void administratorCannotChangeRolePolicies() {
        final RoleManagementService service = service(role(), resource());

        assertThrows(
            AuthorizationDeniedException.class,
            () -> service.updateRole(
                SecurityRoleNames.ADMIN_RIGHTS,
                Set.of("Market"),
                user(SecurityRoleNames.ADMIN_RIGHTS)
            )
        );
    }

    @Test
    void allRightsUserCanChangeRolePolicies() {
        final RoleEntity role = role();
        final ResourceEntity resource = resource();
        final RoleManagementService service = service(role, resource);

        service.updateRole(
            SecurityRoleNames.ADMIN_RIGHTS,
            Set.of(resource.getName()),
            user(SecurityRoleNames.ALL_RIGHTS)
        ).orElseThrow();

        assertEquals(Set.of(resource), role.getResources());
    }

    private RoleManagementService service(RoleEntity role, ResourceEntity resource) {
        final RoleDao roleDao = proxy(RoleDao.class, (method, arguments) -> {
            if ("findByNameWithResourcesForUpdate".equals(method)) {
                return Optional.of(role);
            }
            if ("save".equals(method)) {
                return arguments[0];
            }
            throw new AssertionError("Unexpected RoleDao call: " + method);
        });
        final ResourceDao resourceDao = proxy(ResourceDao.class, (method, arguments) -> {
            throw new AssertionError("Unexpected ResourceDao call: " + method);
        });
        final AccessControlAssignmentService assignments = new AccessControlAssignmentService() {
            @Override
            public Set<ResourceEntity> resolveResources(Set<String> resourceNames) {
                return Set.of(resource);
            }
        };
        return new RoleManagementService(roleDao, resourceDao, assignments, new NoOpAuditService());
    }

    private RoleEntity role() {
        final RoleEntity role = new RoleEntity();
        role.setId(1L);
        role.setName(SecurityRoleNames.ADMIN_RIGHTS);
        return role;
    }

    private ResourceEntity resource() {
        final ResourceEntity resource = new ResourceEntity();
        resource.setId(2L);
        resource.setName("Market");
        resource.setPathPrefix("market");
        resource.setHttpMethod("GET");
        return resource;
    }

    private AuthUserDto user(String roleName) {
        return new AuthUserDto(1L, "operator", Set.of(roleName));
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, DaoInvocation invocation) {
        return (T) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{type},
            (proxy, method, arguments) -> invocation.invoke(method.getName(), arguments)
        );
    }

    @FunctionalInterface
    private interface DaoInvocation {
        Object invoke(String method, Object[] arguments);
    }

    private static final class NoOpAuditService extends AuthenticationAuditService {
        @Override
        public void record(String event, String outcome, String subject, String sourceAddress, String reason) {
        }
    }
}
