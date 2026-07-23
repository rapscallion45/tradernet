package com.tradernet.user;

import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.jpa.entities.RoleEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.List;
import java.util.Objects;

/**
 * Persists API-owned authorization policies and seeds defaults for new resources.
 */
@Stateless
public class AuthorizationPolicyRegistrationService implements AuthorizationPolicyRegistrar {

    @EJB
    private ResourceDao resourceDao;

    @EJB
    private RoleDao roleDao;

    public AuthorizationPolicyRegistrationService() {
    }

    AuthorizationPolicyRegistrationService(ResourceDao resourceDao, RoleDao roleDao) {
        this.resourceDao = resourceDao;
        this.roleDao = roleDao;
    }

    @Override
    public void registerPolicies(List<AuthorizationPolicyDefinition> policies) {
        if (policies == null) {
            throw new IllegalArgumentException("authorization policies are required");
        }
        for (AuthorizationPolicyDefinition policy : policies) {
            registerPolicy(policy);
        }
    }

    private void registerPolicy(AuthorizationPolicyDefinition policy) {
        if (policy == null) {
            throw new IllegalArgumentException("authorization policy is required");
        }
        final ResourceRegistration registration = ensureResource(policy);
        if (!registration.created) {
            return;
        }

        for (String roleName : policy.getDefaultRoleNames()) {
            final RoleEntity role = roleDao.findByNameWithResourcesForUpdate(roleName)
                .orElseThrow(() -> new IllegalStateException("Required security role is missing: " + roleName));
            if (role.getResources().stream().noneMatch(existing -> sameResource(existing, registration.resource))) {
                role.addResource(registration.resource);
                roleDao.save(role);
            }
        }
    }

    private ResourceRegistration ensureResource(AuthorizationPolicyDefinition policy) {
        final ResourceEntity existing = resourceDao.findByName(policy.getName()).orElse(null);
        if (existing != null) {
            if (!Objects.equals(existing.getPathPrefix(), policy.getPathPrefix())
                || !Objects.equals(existing.getHttpMethod(), policy.getHttpMethod())) {
                existing.setPathPrefix(policy.getPathPrefix());
                existing.setHttpMethod(policy.getHttpMethod());
                resourceDao.save(existing);
            }
            return new ResourceRegistration(existing, false);
        }

        final ResourceEntity resource = new ResourceEntity();
        resource.setName(policy.getName());
        resource.setPathPrefix(policy.getPathPrefix());
        resource.setHttpMethod(policy.getHttpMethod());
        return new ResourceRegistration(resourceDao.save(resource), true);
    }

    private boolean sameResource(ResourceEntity left, ResourceEntity right) {
        if (left.getId() != null && right.getId() != null) {
            return left.getId().equals(right.getId());
        }
        return Objects.equals(left.getName(), right.getName())
            && Objects.equals(left.getPathPrefix(), right.getPathPrefix());
    }

    private static final class ResourceRegistration {
        private final ResourceEntity resource;
        private final boolean created;

        private ResourceRegistration(ResourceEntity resource, boolean created) {
            this.resource = resource;
            this.created = created;
        }
    }
}
