package com.tradernet.user;

import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.user.dto.RoleDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Owns security role resource-assignment workflows.
 */
@Stateless
public class RoleManagementService {

    @EJB
    private RoleDao roleDao;

    @EJB
    private ResourceDao resourceDao;

    @EJB
    private AuthorizationService authorizationService;

    public List<RoleDto> getRoles() {
        return roleDao.findAllWithResources().stream()
            .map(UserDtoMapper::toRole)
            .collect(Collectors.toList());
    }

    public Optional<RoleDto> getRole(String name) {
        return getRoleEntity(name).map(UserDtoMapper::toRole);
    }

    public List<String> getResourceNames() {
        return resourceDao.findAll().stream()
            .map(ResourceEntity::getName)
            .sorted()
            .collect(Collectors.toList());
    }

    public Optional<RoleDto> updateRole(String name, Set<String> resourceNames) {
        return getRoleEntity(name)
            .map(role -> {
                role.setResources(resolveResources(resourceNames));
                roleDao.save(role);
                authorizationService.invalidate();
                return UserDtoMapper.toRole(role);
            });
    }

    private Optional<RoleEntity> getRoleEntity(String name) {
        return roleDao.findByNameWithResources(name);
    }

    private Set<ResourceEntity> resolveResources(Set<String> resourceNames) {
        if (resourceNames == null || resourceNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<ResourceEntity> resources = new HashSet<>();
        for (String resourceName : resourceNames) {
            if (resourceName == null || resourceName.isBlank()) {
                throw new IllegalArgumentException("Resource name is required");
            }
            ResourceEntity resource = resourceDao.findByName(resourceName)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourceName));
            resources.add(resource);
        }
        return resources;
    }
}
