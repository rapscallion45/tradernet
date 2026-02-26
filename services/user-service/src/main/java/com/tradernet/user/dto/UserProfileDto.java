package com.tradernet.user.dto;

import com.tradernet.jpa.entities.UserEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lightweight user payload for API responses.
 */
public class UserProfileDto {

    private long id;
    private String username;
    private String fullName;
    private Set<String> roleNames = new HashSet<>();

    public UserProfileDto() {
    }

    public UserProfileDto(long id, String username, String fullName, Set<String> roleNames) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.roleNames = roleNames;
    }

    public static UserProfileDto fromUser(UserEntity user) {
        Set<String> groupRoleNames = user.getGroupsIncParents().stream()
            .flatMap(group -> group.getRoles().stream())
            .map(role -> role.getName())
            .collect(Collectors.toSet());

        Set<String> effectiveRoleNames = new HashSet<>(user.getRoleNames());
        effectiveRoleNames.addAll(groupRoleNames);

        return new UserProfileDto(
            user.getPk(),
            user.getUsername(),
            user.getFullName(),
            effectiveRoleNames
        );
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Set<String> getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(Set<String> roleNames) {
        this.roleNames = roleNames;
    }
}
