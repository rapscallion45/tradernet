package com.tradernet.user;

import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.dao.UserDao;
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

    private static final String SUPER_USER_ROLE = "SUPER USER";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String STANDARD_ROLE = "STANDARD";

    private static final String DEFAULT_SUPER_USER_USERNAME =
        System.getProperty("tradernet.bootstrap.superuser.username", "admin");
    private static final String DEFAULT_SUPER_USER_PASSWORD =
        System.getProperty("tradernet.bootstrap.superuser.password", "ChangeMe");

    @EJB
    private RoleDao roleDao;

    @EJB
    private UserDao userDao;

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Resource(lookup = "java:/jdbc/TradernetDS")
    private DataSource dataSource;

    @PostConstruct
    void bootstrap() {
        initializeSchemaForH2();

        RoleEntity superUserRole = ensureRole(SUPER_USER_ROLE);
        ensureRole(ADMIN_ROLE);
        ensureRole(STANDARD_ROLE);

        UserEntity superUser = userDao.findByUsername(DEFAULT_SUPER_USER_USERNAME)
            .orElseGet(() -> createSuperUser(DEFAULT_SUPER_USER_USERNAME, DEFAULT_SUPER_USER_PASSWORD));

        if (!superUser.getRoleNames().contains(SUPER_USER_ROLE)) {
            superUser.addRole(superUserRole);
            userDao.save(superUser);
            LOG.info("Ensured '{}' role for bootstrap user '{}'.", SUPER_USER_ROLE, DEFAULT_SUPER_USER_USERNAME);
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

    private UserEntity createSuperUser(String username, String password) {
        UserEntity user = new UserEntity(username);
        user.setPk(nextUserId());
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.addRole(ensureRole(SUPER_USER_ROLE));
        userDao.save(user);
        LOG.info("Created bootstrap super user '{}'.", username);
        return user;
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
