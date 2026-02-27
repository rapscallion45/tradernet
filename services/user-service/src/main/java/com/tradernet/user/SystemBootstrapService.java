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
import javax.sql.DataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
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

    @EJB
    private RoleDao roleDao;

    @EJB
    private GroupDao groupDao;

    @EJB
    private ResourceDao resourceDao;

    @EJB
    private UserDao userDao;

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Resource(lookup = "java:/jdbc/TradernetDS")
    private DataSource dataSource;

    @PostConstruct
    void bootstrap() {
        initializeSchemaForH2();

        RoleEntity allRightsRole = ensureRole(ALL_RIGHTS_ROLE);
        RoleEntity adminRightsRole = ensureRole(ADMIN_RIGHTS_ROLE);
        RoleEntity standardRightsRole = ensureRole(STANDARD_RIGHTS_ROLE);

        List<ResourceEntity> resources = ensureProtectedResources();
        ensureRoleHasResources(allRightsRole, resources);
        ensureRoleHasResources(adminRightsRole, resources.stream().filter(resource ->
            "users".equals(resource.getPathPrefix()) || "groups".equals(resource.getPathPrefix())
        ).collect(Collectors.toList()));

        GroupEntity superUsersGroup = ensureGroup(SUPER_USERS_GROUP);
        GroupEntity administratorsGroup = ensureGroup(ADMINISTRATORS_GROUP);
        GroupEntity standardUsersGroup = ensureGroup(STANDARD_USERS_GROUP);

        assignRoleToGroup(superUsersGroup, allRightsRole);
        assignRoleToGroup(administratorsGroup, adminRightsRole);
        assignRoleToGroup(standardUsersGroup, standardRightsRole);

        UserEntity superUser = userDao.findByUsername(DEFAULT_SUPER_USER_USERNAME)
            .orElseGet(() -> createSuperUser(DEFAULT_SUPER_USER_USERNAME, DEFAULT_PASSWORD));

        ensureBootstrapCredentials(superUser, DEFAULT_PASSWORD);

        if (!superUser.getGroups().stream().anyMatch(group -> SUPER_USERS_GROUP.equals(group.getName()))) {
            superUser.addGroup(superUsersGroup);
            userDao.save(superUser);
            LOG.info("Ensured '{}' group membership for bootstrap user '{}'.", SUPER_USERS_GROUP, DEFAULT_SUPER_USER_USERNAME);
        }

        UserEntity adminUser = userDao.findByUsername(DEFAULT_ADMIN_USERNAME)
            .orElseGet(() -> createUser(DEFAULT_ADMIN_USERNAME, "Admin", DEFAULT_PASSWORD));
        ensureUserInGroup(adminUser, administratorsGroup, ADMINISTRATORS_GROUP);

        UserEntity standardUser = userDao.findByUsername(DEFAULT_STANDARD_USERNAME)
            .orElseGet(() -> createUser(DEFAULT_STANDARD_USERNAME, "Standard User", DEFAULT_PASSWORD));
        ensureUserInGroup(standardUser, standardUsersGroup, STANDARD_USERS_GROUP);

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

    private UserEntity createSuperUser(String username, String password) {
        return createUser(username, "Super User", password);
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

    private void ensureBootstrapCredentials(UserEntity user, String password) {
        user.setFullName("Super User");
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setChangePasswordNextLogin(true);
        userDao.save(user);
        LOG.info("Reset bootstrap credentials for user '{}'.", user.getUsername());
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
        ensureResource("Trades", "trades");
        ensureResource("Signals", "signals");
        ensureResource("Market", "market");
        ensureResource("User Properties", "user-properties");
        ensureResource("Passwords", "passwords");
        ensureResource("Health", "health");
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

    private void ensureRoleHasResources(RoleEntity role, List<ResourceEntity> resources) {
        role.setResources(new java.util.HashSet<>(resources));
        roleDao.save(role);
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

    private void initializeSchemaForH2() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            if (!"H2".equalsIgnoreCase(metaData.getDatabaseProductName())) {
                return;
            }

            runSqlScript(connection, "/META-INF/db/schema.sql");
            runSqlScript(connection, "/META-INF/db/dev-seed.sql");
            LOG.info("Initialized schema and seed data for H2 datasource.");
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to initialize H2 schema.", e);
        }
    }

    private void runSqlScript(Connection connection, String scriptPath) throws IOException, SQLException {
        try (InputStream stream = getClass().getResourceAsStream(scriptPath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing SQL script on classpath: " + scriptPath);
            }

            String script;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                script = reader.lines().collect(Collectors.joining("\n"));
            }

            for (String statementSql : splitStatements(script)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(statementSql);
                }
            }
        }
    }

    private List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : script.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }

            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String sql = current.toString().trim();
                statements.add(sql.substring(0, sql.length() - 1));
                current.setLength(0);
            }
        }

        String trailing = current.toString().trim();
        if (!trailing.isEmpty()) {
            statements.add(trailing);
        }

        return statements;
    }
}
