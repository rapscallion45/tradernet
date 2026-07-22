package com.tradernet.user;

import jakarta.ejb.Local;

/**
 * Initialization barrier for components that depend on built-in roles.
 */
@Local
public interface IdentityBootstrapReadiness {

    void ensureInitialized();
}
