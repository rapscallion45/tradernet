package com.tradernet.api.resources.dto;

import com.tradernet.jpa.entities.GroupEntity;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Group API payload with users and assigned security roles.
 */
public class GroupDto {

    private Long id;
    private String name;
    private Set<String> usernames;
    private Set<String> roleNames;

    public GroupDto() {
    }

    public GroupDto(Long id, String name, Set<String> usernames, Set<String> roleNames) {
        this.id = id;
        this.name = name;
        this.usernames = usernames;
        this.roleNames = roleNames;
    }

    public static GroupDto fromEntity(GroupEntity group) {
        return new GroupDto(
            group.getId(),
            group.getName(),
            group.getUsers().stream().map(user -> user.getUsername()).collect(Collectors.toSet()),
            group.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet())
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<String> getUsernames() {
        return usernames;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUsernames(Set<String> usernames) {
        this.usernames = usernames;
    }

    public Set<String> getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(Set<String> roleNames) {
        this.roleNames = roleNames;
    }
}
