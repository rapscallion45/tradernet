package com.tradernet.user;

import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.jpa.entities.RoleEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationPolicyRegistrationServiceTest {

    @Test
    void seedsDefaultRolesOnlyWhenTheApiResourceIsFirstCreated() {
        final Map<String, ResourceEntity> resources = new LinkedHashMap<>();
        final Map<String, RoleEntity> roles = Map.of(
            SecurityRoleNames.ALL_RIGHTS, role(SecurityRoleNames.ALL_RIGHTS),
            SecurityRoleNames.STANDARD_RIGHTS, role(SecurityRoleNames.STANDARD_RIGHTS)
        );
        final AuthorizationPolicyRegistrationService service = new AuthorizationPolicyRegistrationService(
            resourceDao(resources),
            roleDao(roles)
        );
        final AuthorizationPolicyDefinition policy = new AuthorizationPolicyDefinition(
            "Orders",
            "orders",
            "*",
            roles.keySet()
        );

        service.registerPolicies(java.util.List.of(policy));

        final ResourceEntity orders = resources.get("Orders");
        assertEquals("orders", orders.getPathPrefix());
        assertTrue(roles.values().stream().allMatch(role -> role.getResources().contains(orders)));

        roles.get(SecurityRoleNames.STANDARD_RIGHTS).setResources(Set.of());
        service.registerPolicies(java.util.List.of(policy));

        assertTrue(roles.get(SecurityRoleNames.STANDARD_RIGHTS).getResources().isEmpty());
    }

    private RoleEntity role(String name) {
        final RoleEntity role = new RoleEntity();
        role.setName(name);
        return role;
    }

    private ResourceDao resourceDao(Map<String, ResourceEntity> resources) {
        final AtomicLong ids = new AtomicLong();
        return proxy(ResourceDao.class, (method, arguments) -> {
            if ("findByName".equals(method)) {
                return Optional.ofNullable(resources.get(arguments[0]));
            }
            if ("save".equals(method)) {
                final ResourceEntity resource = (ResourceEntity) arguments[0];
                if (resource.getId() == null) {
                    resource.setId(ids.incrementAndGet());
                }
                resources.put(resource.getName(), resource);
                return resource;
            }
            throw new AssertionError("Unexpected ResourceDao call: " + method);
        });
    }

    private RoleDao roleDao(Map<String, RoleEntity> roles) {
        return proxy(RoleDao.class, (method, arguments) -> {
            if ("findByNameWithResourcesForUpdate".equals(method)) {
                return Optional.ofNullable(roles.get(arguments[0]));
            }
            if ("save".equals(method)) {
                return arguments[0];
            }
            throw new AssertionError("Unexpected RoleDao call: " + method);
        });
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
}
