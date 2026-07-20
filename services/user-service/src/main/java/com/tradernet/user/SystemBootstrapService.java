package com.tradernet.user;

import com.tradernet.jpa.dao.GroupDao;
import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.dao.UserDao;
import com.tradernet.jpa.entities.GroupEntity;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.jpa.entities.UserEntity;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * Bootstraps required identity data for every environment.
 */
@Singleton
@Startup
public class SystemBootstrapService {

    private static final Logger LOG = LoggerFactory.getLogger(SystemBootstrapService.class);

    private static final String ALL_RIGHTS_ROLE = "ALL Rights";
    private static final String ADMIN_RIGHTS_ROLE = "Admin Rights";
    private static final String STANDARD_RIGHTS_ROLE = "Standard Rights";

    private static final String SUPER_USERS_GROUP = "Super Users";
    private static final String ADMINISTRATORS_GROUP = "Administrators";
    private static final String STANDARD_USERS_GROUP = "Standard Users";

    private static final String DEFAULT_SUPER_USER_USERNAME = "superuser";
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_STANDARD_USERNAME = "standard";
    private static final String DEFAULT_PASSWORD = "changeme";
    private static final String PASSWORD_HASH_FORMAT_PROBE = "tradernet-password-hash-format-probe";
    private static final String BOOTSTRAP_DEFAULT_PASSWORD_PROPERTY = "tradernet.bootstrap.defaultPassword";
    private static final String BOOTSTRAP_DEFAULT_PASSWORD_ENV = "TRADERNET_BOOTSTRAP_DEFAULT_PASSWORD";
    private static final String BOOTSTRAP_ALLOW_DEFAULT_PASSWORD_PROPERTY = "tradernet.bootstrap.allowDefaultPassword";
    private static final String BOOTSTRAP_ALLOW_DEFAULT_PASSWORD_ENV = "TRADERNET_BOOTSTRAP_ALLOW_DEFAULT_PASSWORD";

    @EJB
    private RoleDao roleDao;

    @EJB
    private GroupDao groupDao;

    @EJB
    private ResourceDao resourceDao;

    @EJB
    private UserDao userDao;

    @EJB
    private AuthorizationService authorizationService;

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @PostConstruct
    void bootstrap() {
        RoleEntity allRightsRole = ensureRole(ALL_RIGHTS_ROLE);
        RoleEntity adminRightsRole = ensureRole(ADMIN_RIGHTS_ROLE);
        RoleEntity standardRightsRole = ensureRole(STANDARD_RIGHTS_ROLE);

        List<ResourceEntity> resources = ensureProtectedResources();
        ensureRoleIncludesResources(allRightsRole, resources);
        ensureRoleIncludesResources(adminRightsRole, resources.stream().filter(resource ->
            "users".equals(resource.getPathPrefix()) || "groups".equals(resource.getPathPrefix())
        ).collect(Collectors.toList()));

        GroupEntity superUsersGroup = ensureGroup(SUPER_USERS_GROUP);
        GroupEntity administratorsGroup = ensureGroup(ADMINISTRATORS_GROUP);
        GroupEntity standardUsersGroup = ensureGroup(STANDARD_USERS_GROUP);

        assignRoleToGroup(superUsersGroup, allRightsRole);
        assignRoleToGroup(administratorsGroup, adminRightsRole);
        assignRoleToGroup(standardUsersGroup, standardRightsRole);

        final String bootstrapPassword = resolveBootstrapPassword();

        UserEntity superUser = ensureBootstrapUser(DEFAULT_SUPER_USER_USERNAME, "Super User", bootstrapPassword);
        if (superUser != null) {
            ensureUserInGroup(superUser, superUsersGroup, SUPER_USERS_GROUP);
        }

        UserEntity adminUser = ensureBootstrapUser(DEFAULT_ADMIN_USERNAME, "Admin", bootstrapPassword);
        if (adminUser != null) {
            ensureUserInGroup(adminUser, administratorsGroup, ADMINISTRATORS_GROUP);
        }

        UserEntity standardUser = ensureBootstrapUser(DEFAULT_STANDARD_USERNAME, "Standard User", bootstrapPassword);
        if (standardUser != null) {
            ensureUserInGroup(standardUser, standardUsersGroup, STANDARD_USERS_GROUP);
        }

    }

    private RoleEntity ensureRole(String roleName) {
        return roleDao.findByName(roleName)
            .orElseGet(() -> {
                RoleEntity role = new RoleEntity();
                role.setName(roleName);
                roleDao.save(role);
                LOG.info("Created required role '{}'.", roleName);
                return role;
            });
    }

    private UserEntity createUser(String username, String fullName, String password) {
        UserEntity user = new UserEntity(username);
        user.setPk(nextUserId());
        user.setFullName(fullName);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setChangePasswordNextLogin(true);
        userDao.save(user);
        LOG.info("Created bootstrap user '{}' ({})", username, fullName);
        return user;
    }

    private UserEntity ensureBootstrapUser(String username, String fullName, String password) {
        return userDao.findByUsername(username)
            .map(user -> {
                if (password == null) {
                    warnIfBootstrapCredentialsCannotBeInitialized(user);
                } else {
                    ensureBootstrapCredentialsWhenMissing(user, fullName, password);
                }
                return user;
            })
            .orElseGet(() -> {
                if (password == null) {
                    LOG.warn("Skipped creating bootstrap user '{}' because no bootstrap password is configured. Set {} "
                            + "or enable {} only for local/dev environments.",
                        username, BOOTSTRAP_DEFAULT_PASSWORD_PROPERTY, BOOTSTRAP_ALLOW_DEFAULT_PASSWORD_PROPERTY);
                    return null;
                }
                return createUser(username, fullName, password);
            });
    }

    private void warnIfBootstrapCredentialsCannotBeInitialized(UserEntity user) {
        if (!hasUsablePasswordHash(user.getPasswordHash())) {
            LOG.warn("Bootstrap user '{}' has no usable password hash, but no bootstrap password is configured. "
                    + "Credentials were not initialized.",
                user.getUsername());
        }
    }

    private void ensureBootstrapCredentialsWhenMissing(UserEntity user, String fullName, String password) {
        if (hasUsablePasswordHash(user.getPasswordHash())) {
            return;
        }

        if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(fullName);
        }
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setChangePasswordNextLogin(true);
        userDao.save(user);
        LOG.info("Initialized bootstrap credentials for user '{}' because the stored password hash was missing or invalid.",
            user.getUsername());
    }

    private boolean hasUsablePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }

        if (!isBcryptHash(passwordHash)) {
            return false;
        }

        try {
            BCrypt.checkpw(PASSWORD_HASH_FORMAT_PROBE, passwordHash);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isBcryptHash(String passwordHash) {
        return passwordHash.startsWith("$2a$")
            || passwordHash.startsWith("$2b$")
            || passwordHash.startsWith("$2y$");
    }

    private GroupEntity ensureGroup(String groupName) {
        return groupDao.findByName(groupName)
            .orElseGet(() -> {
                GroupEntity group = new GroupEntity();
                group.setName(groupName);
                groupDao.save(group);
                return groupDao.findByName(groupName).orElse(group);
            });
    }

    private List<ResourceEntity> ensureProtectedResources() {
        ensureResource("Users", "users");
        ensureResource("Groups", "groups");
        ensureResource("Security Roles", "roles");
        ensureResource("Orders", "orders");
        ensureResource("Portfolio", "portfolio");
        ensureResource("Trades", "trades");
        ensureResource("Market", "market");
        return resourceDao.findAll();
    }

    private ResourceEntity ensureResource(String name, String pathPrefix) {
        return resourceDao.findByName(name)
            .orElseGet(() -> {
                ResourceEntity resource = new ResourceEntity();
                resource.setName(name);
                resource.setPathPrefix(pathPrefix);
                resourceDao.save(resource);
                return resourceDao.findByName(name).orElse(resource);
            });
    }

    private void ensureRoleIncludesResources(RoleEntity role, List<ResourceEntity> resources) {
        boolean changed = false;
        for (ResourceEntity resource : resources) {
            if (!hasResource(role, resource)) {
                role.addResource(resource);
                changed = true;
            }
        }

        if (changed) {
            roleDao.save(role);
            authorizationService.invalidate();
        }
    }

    private boolean hasResource(RoleEntity role, ResourceEntity resource) {
        return role.getResources().stream()
            .anyMatch(existing -> sameResource(existing, resource));
    }

    private boolean sameResource(ResourceEntity existing, ResourceEntity requested) {
        if (existing.getId() != null && requested.getId() != null) {
            return existing.getId().equals(requested.getId());
        }
        return Objects.equals(existing.getPathPrefix(), requested.getPathPrefix())
            && Objects.equals(existing.getName(), requested.getName());
    }

    private void assignRoleToGroup(GroupEntity group, RoleEntity role) {
        if (group.getRoles().stream().noneMatch(existing -> role.getName().equals(existing.getName()))) {
            group.addRole(role);
            groupDao.save(group);
        }
    }

    private void ensureUserInGroup(UserEntity user, GroupEntity group, String groupName) {
        if (user.getGroups().stream().noneMatch(existing -> groupName.equals(existing.getName()))) {
            user.addGroup(group);
            userDao.save(user);
            LOG.info("Ensured '{}' group membership for bootstrap user '{}'.", groupName, user.getUsername());
        }
    }

    private long nextUserId() {
        Long currentMax = entityManager.createQuery("SELECT COALESCE(MAX(u.id), 0) FROM UserEntity u", Long.class)
            .getSingleResult();
        return currentMax + 1;
    }

    private String resolveBootstrapPassword() {
        final String configuredPassword = firstNonBlank(
            System.getProperty(BOOTSTRAP_DEFAULT_PASSWORD_PROPERTY),
            System.getenv(BOOTSTRAP_DEFAULT_PASSWORD_ENV)
        );
        if (configuredPassword != null) {
            return configuredPassword;
        }

        if (isDefaultPasswordAllowed()) {
            LOG.warn("Using insecure bootstrap password fallback because {} or {} is enabled. "
                    + "Use {} or {} for non-local environments.",
                BOOTSTRAP_ALLOW_DEFAULT_PASSWORD_PROPERTY,
                BOOTSTRAP_ALLOW_DEFAULT_PASSWORD_ENV,
                BOOTSTRAP_DEFAULT_PASSWORD_PROPERTY,
                BOOTSTRAP_DEFAULT_PASSWORD_ENV);
            return DEFAULT_PASSWORD;
        }

        return null;
    }

    private boolean isDefaultPasswordAllowed() {
        return Boolean.parseBoolean(firstNonBlank(
            System.getProperty(BOOTSTRAP_ALLOW_DEFAULT_PASSWORD_PROPERTY),
            System.getenv(BOOTSTRAP_ALLOW_DEFAULT_PASSWORD_ENV),
            "false"
        ));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
