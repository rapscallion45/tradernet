package com.tradernet.api.resources.dto;

import java.util.Set;

/**
 * Payload for updating group user/role assignments.
 */
public class UpdateGroupRequestDto {

    private Set<String> usernames;
    private Set<String> roleNames;

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
