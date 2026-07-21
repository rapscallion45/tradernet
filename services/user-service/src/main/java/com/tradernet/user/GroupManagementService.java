package com.tradernet.user;

import com.tradernet.jpa.dao.GroupDao;
import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.entities.GroupEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.user.dto.GroupDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.HashSet;
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
    private RoleDao roleDao;

    @EJB
    private UserService userService;

    public List<GroupDto> getGroups() {
        return groupDao.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public Optional<GroupDto> getGroup(long id) {
        return groupDao.findById(id).map(this::toDto);
    }

    public Optional<GroupDto> updateGroup(long id, Set<String> usernames, Set<String> roleNames) {
        return groupDao.findById(id)
            .map(group -> {
                group.setUsers(resolveUsers(usernames));
                group.setRoles(resolveRoles(roleNames));
                groupDao.save(group);
                return toDto(group);
            });
    }

    private GroupDto toDto(GroupEntity group) {
        return UserDtoMapper.toGroup(initializeGroup(group));
    }

    private GroupEntity initializeGroup(GroupEntity group) {
        group.getUsers().size();
        group.getRoles().size();
        return group;
    }

    private Set<UserEntity> resolveUsers(Set<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return new HashSet<>();
        }

        Set<UserEntity> users = new HashSet<>();
        for (String username : usernames) {
            if (username == null || username.isBlank()) {
                throw new InvalidAccessControlAssignmentException("Username is required");
            }
            UserEntity user = userService.findByUsername(username)
                .orElseThrow(() -> new InvalidAccessControlAssignmentException("User not found: " + username));
            users.add(user);
        }
        return users;
    }

    private Set<RoleEntity> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<RoleEntity> roles = new HashSet<>();
        for (String roleName : roleNames) {
            if (roleName == null || roleName.isBlank()) {
                throw new InvalidAccessControlAssignmentException("Role name is required");
            }
            RoleEntity role = roleDao.findByName(roleName)
                .orElseThrow(() -> new InvalidAccessControlAssignmentException("Role not found: " + roleName));
            roles.add(role);
        }
        return roles;
    }
}
