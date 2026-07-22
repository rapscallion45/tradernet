package com.tradernet.user;

import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.user.dto.AuthUserDto;
import com.tradernet.user.dto.RoleDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Owns security role resource-assignment workflows.
 */
@Stateless
public class RoleManagementService implements RoleManagementOperations {

    @EJB
    private RoleDao roleDao;

    @EJB
    private ResourceDao resourceDao;

    @EJB
    private AccessControlAssignmentService assignmentService;

    @EJB
    private AuthenticationAudit auditService;

    public RoleManagementService() {
    }

    RoleManagementService(
        RoleDao roleDao,
        ResourceDao resourceDao,
        AccessControlAssignmentService assignmentService,
        AuthenticationAudit auditService
    ) {
        this.roleDao = roleDao;
        this.resourceDao = resourceDao;
        this.assignmentService = assignmentService;
        this.auditService = auditService;
    }

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

    public Optional<RoleDto> updateRole(String name, Set<String> resourceNames, AuthUserDto actor) {
        if (!SecurityRoleNames.hasRole(actor, SecurityRoleNames.ALL_RIGHTS)) {
            auditService.record(
                "authorization_change",
                "rejected",
                actor == null ? null : actor.getUsername(),
                null,
                "role_administration_requires_all_rights"
            );
            throw new AuthorizationDeniedException("Role administration requires ALL Rights");
        }
        return roleDao.findByNameWithResourcesForUpdate(name)
            .map(role -> {
                role.setResources(assignmentService.resolveResources(resourceNames));
                roleDao.save(role);
                auditService.record(
                    "authorization_change",
                    "success",
                    actor.getUsername(),
                    null,
                    "role_" + role.getName()
                );
                return UserDtoMapper.toRole(role);
            });
    }

    private Optional<RoleEntity> getRoleEntity(String name) {
        return roleDao.findByNameWithResources(name);
    }

}
