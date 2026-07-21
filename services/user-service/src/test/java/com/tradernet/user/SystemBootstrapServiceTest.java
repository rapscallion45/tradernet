package com.tradernet.user;

import com.tradernet.jpa.dao.GroupDao;
import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.dao.UserDao;
import com.tradernet.jpa.entities.GroupEntity;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.jpa.entities.UserEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemBootstrapServiceTest {

    @Test
    void restartDoesNotRestoreRemovedBootstrapAssignments() {
        final Map<String, RoleEntity> roles = Map.of(
            SecurityRoleNames.ALL_RIGHTS, role(SecurityRoleNames.ALL_RIGHTS),
            SecurityRoleNames.ADMIN_RIGHTS, role(SecurityRoleNames.ADMIN_RIGHTS),
            SecurityRoleNames.STANDARD_RIGHTS, role(SecurityRoleNames.STANDARD_RIGHTS)
        );
        final Map<String, GroupEntity> groups = Map.of(
            "Super Users", group("Super Users"),
            "Administrators", group("Administrators"),
            "Standard Users", group("Standard Users")
        );
        final Map<String, UserEntity> users = Map.of(
            "superuser", user("superuser"),
            "admin", user("admin"),
            "standard", user("standard")
        );
        final Map<String, ResourceEntity> resources = protectedResources();
        final SystemBootstrapService service = new SystemBootstrapService(
            lookupDao(RoleDao.class, "findByName", roles),
            lookupDao(GroupDao.class, "findByName", groups),
            resourceDao(resources),
            lookupDao(UserDao.class, "findByUsername", users),
            noBootstrapPasswords(),
            usableHashService(),
            noOpCredentialPolicy()
        );

        service.bootstrap();

        assertTrue(roles.values().stream().allMatch(role -> role.getResources().isEmpty()));
        assertTrue(groups.values().stream().allMatch(group -> group.getRoles().isEmpty()));
        assertTrue(users.values().stream().allMatch(user -> user.getGroups().isEmpty()));
    }

    private Map<String, ResourceEntity> protectedResources() {
        final Map<String, ResourceEntity> resources = new LinkedHashMap<>();
        addResource(resources, "Users", "users", "*");
        addResource(resources, "Groups", "groups", "*");
        addResource(resources, "Security Roles", "roles", "*");
        addResource(resources, "Orders", "orders", "*");
        addResource(resources, "Portfolio", "portfolio", "*");
        addResource(resources, "Trades", "trades", "*");
        addResource(resources, "Market", "market", "GET");
        addResource(resources, "Market Context Administration", "market/context", "POST");
        return resources;
    }

    private void addResource(Map<String, ResourceEntity> resources, String name, String path, String method) {
        final ResourceEntity resource = new ResourceEntity();
        resource.setName(name);
        resource.setPathPrefix(path);
        resource.setHttpMethod(method);
        resources.put(name, resource);
    }

    private RoleEntity role(String name) {
        final RoleEntity role = new RoleEntity();
        role.setName(name);
        return role;
    }

    private GroupEntity group(String name) {
        final GroupEntity group = new GroupEntity();
        group.setName(name);
        return group;
    }

    private UserEntity user(String username) {
        final UserEntity user = new UserEntity(username);
        user.setPasswordHash("usable-hash");
        return user;
    }

    private ResourceDao resourceDao(Map<String, ResourceEntity> resources) {
        return proxy(ResourceDao.class, (method, arguments) -> {
            if ("findByName".equals(method)) {
                return Optional.ofNullable(resources.get(arguments[0]));
            }
            if ("findAll".equals(method)) {
                return List.copyOf(resources.values());
            }
            throw new AssertionError("Bootstrap unexpectedly wrote an existing resource through " + method);
        });
    }

    private <T> T lookupDao(Class<T> type, String lookupMethod, Map<String, ?> values) {
        return proxy(type, (method, arguments) -> {
            if (lookupMethod.equals(method)) {
                return Optional.ofNullable(values.get(arguments[0]));
            }
            throw new AssertionError("Bootstrap unexpectedly called " + type.getSimpleName() + "." + method);
        });
    }

    private UserSecurityConfiguration noBootstrapPasswords() {
        return new UserSecurityConfiguration() {
            @Override
            public String getBootstrapPassword(String username) {
                return null;
            }

            @Override
            public boolean isInsecureBootstrapPasswordEnabled() {
                return false;
            }
        };
    }

    private PasswordSecurityService usableHashService() {
        return new PasswordSecurityService() {
            @Override
            public boolean isUsableHash(String passwordHash) {
                return true;
            }
        };
    }

    private BootstrapCredentialPolicy noOpCredentialPolicy() {
        return new BootstrapCredentialPolicy() {
            @Override
            public void verifyPersistedCredential(UserEntity user) {
            }
        };
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
