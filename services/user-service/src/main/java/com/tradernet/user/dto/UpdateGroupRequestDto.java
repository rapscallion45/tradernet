package com.tradernet.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Payload for updating group user/role assignments.
 */
public class UpdateGroupRequestDto {

    @NotNull(message = "usernames is required")
    private Set<@NotBlank(message = "Username is required") String> usernames;

    @NotNull(message = "roleNames is required")
    private Set<@NotBlank(message = "Role name is required") String> roleNames;

    public Set<String> getUsernames() {
        return usernames;
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
