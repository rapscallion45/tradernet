package com.tradernet.user;

import com.tradernet.jpa.dao.GroupDao;
import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.dao.UserDao;
import com.tradernet.jpa.entities.GroupEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.jpa.entities.UserEntity;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstraps required identity data for every environment.
 */
@Singleton
@Startup
public class SystemBootstrapService implements IdentityBootstrapReadiness {

    private static final Logger LOG = LoggerFactory.getLogger(SystemBootstrapService.class);

    private static final String SUPER_USERS_GROUP = "Super Users";
    private static final String ADMINISTRATORS_GROUP = "Administrators";
    private static final String STANDARD_USERS_GROUP = "Standard Users";

    private static final String DEFAULT_SUPER_USER_USERNAME = "superuser";
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_STANDARD_USERNAME = "standard";
    private static final String BOOTSTRAP_ALLOW_DEFAULT_PASSWORD_PROPERTY = "tradernet.bootstrap.allowDefaultPassword";
    private static final String BOOTSTRAP_ALLOW_DEFAULT_PASSWORD_ENV = "TRADERNET_BOOTSTRAP_ALLOW_DEFAULT_PASSWORD";

    @EJB
    private RoleDao roleDao;

    @EJB
    private GroupDao groupDao;

    @EJB
    private UserDao userDao;

    @EJB
    private UserSecurityPolicy configuration;

    @EJB
    private PasswordSecurityService passwordSecurityService;

    @EJB
    private BootstrapCredentialPolicy bootstrapCredentialPolicy;

    public SystemBootstrapService() {
    }

    SystemBootstrapService(
        RoleDao roleDao,
        GroupDao groupDao,
        UserDao userDao,
        UserSecurityPolicy configuration,
        PasswordSecurityService passwordSecurityService,
        BootstrapCredentialPolicy bootstrapCredentialPolicy
    ) {
        this.roleDao = roleDao;
        this.groupDao = groupDao;
        this.userDao = userDao;
        this.configuration = configuration;
        this.passwordSecurityService = passwordSecurityService;
        this.bootstrapCredentialPolicy = bootstrapCredentialPolicy;
    }

    @PostConstruct
    void bootstrap() {
        BootstrapResult<RoleEntity> allRightsRole = ensureRole(SecurityRoleNames.ALL_RIGHTS);
        BootstrapResult<RoleEntity> adminRightsRole = ensureRole(SecurityRoleNames.ADMIN_RIGHTS);
        BootstrapResult<RoleEntity> standardRightsRole = ensureRole(SecurityRoleNames.STANDARD_RIGHTS);

        BootstrapResult<GroupEntity> superUsersGroup = ensureGroup(SUPER_USERS_GROUP);
        BootstrapResult<GroupEntity> administratorsGroup = ensureGroup(ADMINISTRATORS_GROUP);
        BootstrapResult<GroupEntity> standardUsersGroup = ensureGroup(STANDARD_USERS_GROUP);

        seedGroupRole(superUsersGroup, allRightsRole.entity);
        seedGroupRole(administratorsGroup, adminRightsRole.entity);
        seedGroupRole(standardUsersGroup, standardRightsRole.entity);

        if (configuration.isInsecureBootstrapPasswordEnabled()) {
            LOG.warn("The insecure local bootstrap password fallback is enabled by {} or {}. "
                    + "Never enable it outside local development.",
                BOOTSTRAP_ALLOW_DEFAULT_PASSWORD_PROPERTY,
                BOOTSTRAP_ALLOW_DEFAULT_PASSWORD_ENV);
        }

        BootstrapResult<UserEntity> superUser = ensureBootstrapUser(
            DEFAULT_SUPER_USER_USERNAME,
            "Super User",
            configuration.getBootstrapPassword(DEFAULT_SUPER_USER_USERNAME)
        );
        if (superUser.created) {
            ensureUserInGroup(superUser.entity, superUsersGroup.entity, SUPER_USERS_GROUP);
        }

        BootstrapResult<UserEntity> adminUser = ensureBootstrapUser(
            DEFAULT_ADMIN_USERNAME,
            "Admin",
            configuration.getBootstrapPassword(DEFAULT_ADMIN_USERNAME)
        );
        if (adminUser.created) {
            ensureUserInGroup(adminUser.entity, administratorsGroup.entity, ADMINISTRATORS_GROUP);
        }

        BootstrapResult<UserEntity> standardUser = ensureBootstrapUser(
            DEFAULT_STANDARD_USERNAME,
            "Standard User",
            configuration.getBootstrapPassword(DEFAULT_STANDARD_USERNAME)
        );
        if (standardUser.created) {
            ensureUserInGroup(standardUser.entity, standardUsersGroup.entity, STANDARD_USERS_GROUP);
        }

    }

    @Override
    public void ensureInitialized() {
        // Calling the startup singleton through this view is the initialization barrier.
    }

    private BootstrapResult<RoleEntity> ensureRole(String roleName) {
        final RoleEntity existing = roleDao.findByName(roleName).orElse(null);
        if (existing != null) {
            return BootstrapResult.existing(existing);
        }
        RoleEntity role = new RoleEntity();
        role.setName(roleName);
        role = roleDao.save(role);
        LOG.info("Created required role '{}'.", roleName);
        return BootstrapResult.created(role);
    }

    private UserEntity createUser(String username, String fullName, String password) {
        validateConfiguredBootstrapPassword(username, password);
        UserEntity user = new UserEntity(username);
        user.setFullName(fullName);
        user.setPasswordHash(passwordSecurityService.hashPassword(password));
        user.setChangePasswordNextLogin(true);
        user = userDao.save(user);
        LOG.info("Created bootstrap user '{}' ({})", username, fullName);
        return user;
    }

    private BootstrapResult<UserEntity> ensureBootstrapUser(String username, String fullName, String password) {
        final UserEntity existing = userDao.findByUsername(username).orElse(null);
        if (existing != null) {
            bootstrapCredentialPolicy.verifyPersistedCredential(existing);
            if (password == null) {
                warnIfBootstrapCredentialsCannotBeInitialized(existing);
            } else {
                ensureBootstrapCredentialsWhenMissing(existing, fullName, password);
            }
            return BootstrapResult.existing(existing);
        }
        if (password == null) {
            LOG.info("Skipped creating optional bootstrap user '{}' because no account-specific password is configured.",
                username);
            return BootstrapResult.absent();
        }
        return BootstrapResult.created(createUser(username, fullName, password));
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

        validateConfiguredBootstrapPassword(user.getUsername(), password);
        if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(fullName);
        }
        user.setPasswordHash(passwordSecurityService.hashPassword(password));
        user.setChangePasswordNextLogin(true);
        userDao.save(user);
        LOG.info("Initialized bootstrap credentials for user '{}' because the stored password hash was missing or invalid.",
            user.getUsername());
    }

    private boolean hasUsablePasswordHash(String passwordHash) {
        return passwordSecurityService.isUsableHash(passwordHash);
    }

    private void validateConfiguredBootstrapPassword(String username, String password) {
        if (!configuration.isUsingInsecureBootstrapPassword(username)) {
            passwordSecurityService.validateNewPassword(username, password);
        }
    }

    private BootstrapResult<GroupEntity> ensureGroup(String groupName) {
        final GroupEntity existing = groupDao.findByName(groupName).orElse(null);
        if (existing != null) {
            return BootstrapResult.existing(existing);
        }
        GroupEntity group = new GroupEntity();
        group.setName(groupName);
        return BootstrapResult.created(groupDao.save(group));
    }

    private void assignRoleToGroup(GroupEntity group, RoleEntity role) {
        if (group.getRoles().stream().noneMatch(existing -> role.getName().equals(existing.getName()))) {
            group.addRole(role);
            groupDao.save(group);
        }
    }

    private void seedGroupRole(BootstrapResult<GroupEntity> group, RoleEntity role) {
        if (group.created) {
            assignRoleToGroup(group.entity, role);
        }
    }

    private void ensureUserInGroup(UserEntity user, GroupEntity group, String groupName) {
        if (user.getGroups().stream().noneMatch(existing -> groupName.equals(existing.getName()))) {
            user.addGroup(group);
            userDao.save(user);
            LOG.info("Ensured '{}' group membership for bootstrap user '{}'.", groupName, user.getUsername());
        }
    }

    private static final class BootstrapResult<T> {
        private final T entity;
        private final boolean created;

        private BootstrapResult(T entity, boolean created) {
            this.entity = entity;
            this.created = created;
        }

        private static <T> BootstrapResult<T> created(T entity) {
            return new BootstrapResult<>(entity, true);
        }

        private static <T> BootstrapResult<T> existing(T entity) {
            return new BootstrapResult<>(entity, false);
        }

        private static <T> BootstrapResult<T> absent() {
            return new BootstrapResult<>(null, false);
        }
    }

}
