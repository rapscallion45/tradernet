package com.tradernet.user;

import jakarta.ejb.Local;

import java.util.List;

/**
 * Cross-module contract for registering an API-owned policy catalog.
 */
@Local
public interface AuthorizationPolicyRegistrar {

    void registerPolicies(List<AuthorizationPolicyDefinition> policies);
}
