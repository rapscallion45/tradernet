package com.tradernet.api;

import com.tradernet.user.AuthorizationPolicyDefinition;
import com.tradernet.user.AuthorizationPolicyRegistrar;
import com.tradernet.user.IdentityBootstrapReadiness;
import com.tradernet.user.SecurityRoleNames;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.List;
import java.util.Set;

/**
 * Declares the authorization policies owned by this REST application.
 */
@Singleton
@Startup
public class ApiAuthorizationPolicyBootstrap {

    private static final Set<String> ALL_ONLY = Set.of(SecurityRoleNames.ALL_RIGHTS);
    private static final Set<String> ADMIN_ONLY = Set.of(
        SecurityRoleNames.ALL_RIGHTS,
        SecurityRoleNames.ADMIN_RIGHTS
    );
    private static final Set<String> STANDARD = Set.of(
        SecurityRoleNames.ALL_RIGHTS,
        SecurityRoleNames.ADMIN_RIGHTS,
        SecurityRoleNames.STANDARD_RIGHTS
    );

    @EJB
    private IdentityBootstrapReadiness identityBootstrapReadiness;

    @EJB
    private AuthorizationPolicyRegistrar registrationService;

    @PostConstruct
    void registerPolicies() {
        identityBootstrapReadiness.ensureInitialized();
        registrationService.registerPolicies(List.of(
            policy("Users", "users", "*", ADMIN_ONLY),
            policy("Groups", "groups", "*", ADMIN_ONLY),
            policy("Security Roles", "roles", "*", ALL_ONLY),
            policy("Orders", "orders", "*", STANDARD),
            policy("Portfolio", "portfolio", "*", STANDARD),
            policy("Trades", "trades", "*", STANDARD),
            policy("Market", "market", "GET", STANDARD),
            policy("Market Context Administration", "market/context", "POST", ADMIN_ONLY)
        ));
    }

    private AuthorizationPolicyDefinition policy(
        String name,
        String pathPrefix,
        String httpMethod,
        Set<String> defaultRoles
    ) {
        return new AuthorizationPolicyDefinition(name, pathPrefix, httpMethod, defaultRoles);
    }
}
