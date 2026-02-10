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

    @PostConstruct
    void bootstrap() {
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
        user.setId(nextUserId());
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
}
