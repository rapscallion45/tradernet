package com.tradernet.user;

import com.tradernet.jpa.dao.GroupDao;
import com.tradernet.jpa.entities.GroupEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.user.dto.AuthUserDto;
import com.tradernet.user.dto.GroupDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Owns group membership and group-role update workflows.
 */
@Stateless
public class GroupManagementService {

    @EJB
    private GroupDao groupDao;

    @EJB
    private AccessControlAssignmentService assignmentService;

    @EJB
    private AuthenticationAuditService auditService;

    public GroupManagementService() {
    }

    GroupManagementService(
        GroupDao groupDao,
        AccessControlAssignmentService assignmentService,
        AuthenticationAuditService auditService
    ) {
        this.groupDao = groupDao;
        this.assignmentService = assignmentService;
        this.auditService = auditService;
    }

    public List<GroupDto> getGroups() {
        return groupDao.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public Optional<GroupDto> getGroup(long id) {
        return groupDao.findById(id).map(this::toDto);
    }

    public Optional<GroupDto> updateGroup(
        long id,
        Set<String> usernames,
        Set<String> roleNames,
        AuthUserDto actor
    ) {
        requireGroupAdministrator(actor);
        return groupDao.findByIdForUpdate(id)
            .map(group -> {
                requirePermittedPrivilegeChange(group, roleNames, actor);
                group.setUsers(assignmentService.resolveUsers(usernames));
                group.setRoles(assignmentService.resolveRoles(roleNames));
                groupDao.save(group);
                auditService.record(
                    "authorization_change",
                    "success",
                    actor.getUsername(),
                    null,
                    "group_" + group.getId()
                );
                return toDto(group);
            });
    }

    private void requireGroupAdministrator(AuthUserDto actor) {
        if (SecurityRoleNames.hasRole(actor, SecurityRoleNames.ALL_RIGHTS)
            || SecurityRoleNames.hasRole(actor, SecurityRoleNames.ADMIN_RIGHTS)) {
            return;
        }
        auditDenied(actor, "group_administration_not_permitted");
        throw new AuthorizationDeniedException("Group administration is not permitted");
    }

    private void requirePermittedPrivilegeChange(GroupEntity group, Set<String> requestedRoleNames, AuthUserDto actor) {
        if (SecurityRoleNames.hasRole(actor, SecurityRoleNames.ALL_RIGHTS)) {
            return;
        }

        final boolean currentlyPrivileged = group.getRoles().stream()
            .map(RoleEntity::getName)
            .anyMatch(SecurityRoleNames.ALL_RIGHTS::equals);
        final boolean requestsPrivilegedRole = requestedRoleNames != null && requestedRoleNames.stream()
            .filter(name -> name != null)
            .map(String::trim)
            .anyMatch(SecurityRoleNames.ALL_RIGHTS::equals);
        if (currentlyPrivileged || requestsPrivilegedRole) {
            auditDenied(actor, "all_rights_assignment_requires_all_rights");
            throw new AuthorizationDeniedException("Only an ALL Rights user may change ALL Rights assignments");
        }
    }

    private void auditDenied(AuthUserDto actor, String reason) {
        auditService.record(
            "authorization_change",
            "rejected",
            actor == null ? null : actor.getUsername(),
            null,
            reason
        );
    }

    private GroupDto toDto(GroupEntity group) {
        return UserDtoMapper.toGroup(group);
    }

}
