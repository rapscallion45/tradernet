package com.tradernet.user;

import com.tradernet.jpa.entities.UserEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Prevents known local-development credentials from surviving in a production bootstrap account.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class BootstrapCredentialPolicy {

    @EJB
    private UserSecurityConfiguration configuration;

    @EJB
    private PasswordSecurityService passwordSecurityService;

    public BootstrapCredentialPolicy() {
    }

    BootstrapCredentialPolicy(
        UserSecurityConfiguration configuration,
        PasswordSecurityService passwordSecurityService
    ) {
        this.configuration = configuration;
        this.passwordSecurityService = passwordSecurityService;
    }

    public void verifyPersistedCredential(UserEntity user) {
        if (user == null || configuration.isInsecureBootstrapPasswordEnabled()) {
            return;
        }
        if (passwordSecurityService.matches(
            configuration.getInsecureBootstrapPassword(),
            user.getPasswordHash()
        )) {
            throw new IllegalStateException(
                "Bootstrap user '" + user.getUsername() + "' still uses the insecure development password"
            );
        }
    }
}
