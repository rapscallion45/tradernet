package com.tradernet.user.dto;

import com.tradernet.jpa.entities.UserEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Lightweight auth user payload.
 */
public class AuthUserDto {

    private long id;
    private String username;
    private Set<String> roleNames = new HashSet<>();

    public AuthUserDto() {
    }

    public AuthUserDto(long id, String username, Set<String> roleNames) {
        this.id = id;
        this.username = username;
        this.roleNames = roleNames;
    }

    public static AuthUserDto fromUser(UserEntity user) {
        return new AuthUserDto(user.getPk(), user.getUsername(), new HashSet<>(user.getRoleNames()));
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

    public Set<String> getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(Set<String> roleNames) {
        this.roleNames = roleNames;
    }
}
