package com.tradernet.user;

import com.tradernet.jpa.entities.GroupEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.user.dto.AuthUserDto;
import com.tradernet.user.dto.GroupDto;
import com.tradernet.user.dto.RoleDto;
import com.tradernet.user.dto.UserProfileDto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

final class UserDtoMapper {

    private UserDtoMapper() {
    }

    static AuthUserDto toAuthUser(UserEntity user) {
        return new AuthUserDto(user.getPk(), user.getUsername(), effectiveRoleNames(user));
    }

    static UserProfileDto toUserProfile(UserEntity user) {
        return new UserProfileDto(
            user.getPk(),
            user.getUsername(),
            user.getFullName(),
            effectiveRoleNames(user)
        );
    }

    static GroupDto toGroup(GroupEntity group) {
        return new GroupDto(
            group.getId(),
            group.getName(),
            group.getUsers().stream()
                .map(UserEntity::getUsername)
                .collect(Collectors.toSet()),
            group.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet())
        );
    }

    static RoleDto toRole(RoleEntity role) {
        return new RoleDto(
            role.getName(),
            role.getResources().stream()
                .map(resource -> resource.getName())
                .collect(Collectors.toSet())
        );
    }

    private static Set<String> effectiveRoleNames(UserEntity user) {
        Set<String> groupRoleNames = user.getGroupsIncParents().stream()
            .flatMap(group -> group.getRoles().stream())
            .map(RoleEntity::getName)
            .collect(Collectors.toSet());

        Set<String> effectiveRoleNames = new HashSet<>(user.getRoleNames());
        effectiveRoleNames.addAll(groupRoleNames);
        return effectiveRoleNames;
    }
}
