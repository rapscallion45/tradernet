package com.tradernet.user;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Persistence-neutral protected-resource policy owned by an API module.
 */
public final class AuthorizationPolicyDefinition {

    private final String name;
    private final String pathPrefix;
    private final String httpMethod;
    private final Set<String> defaultRoleNames;

    public AuthorizationPolicyDefinition(
        String name,
        String pathPrefix,
        String httpMethod,
        Set<String> defaultRoleNames
    ) {
        this.name = name;
        this.pathPrefix = pathPrefix;
        this.httpMethod = httpMethod;
        this.defaultRoleNames = Collections.unmodifiableSet(new LinkedHashSet<>(defaultRoleNames));
    }

    public String getName() { return name; }
    public String getPathPrefix() { return pathPrefix; }
    public String getHttpMethod() { return httpMethod; }
    public Set<String> getDefaultRoleNames() { return defaultRoleNames; }
}
