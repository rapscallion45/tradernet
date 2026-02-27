package com.tradernet.api.resources.dto;

import java.util.Set;

/**
 * Payload for updating allowed resources on a role.
 */
public class UpdateRoleRequestDto {

    private Set<String> resourceNames;

    public Set<String> getResourceNames() {
        return resourceNames;
    }

    public void setResourceNames(Set<String> resourceNames) {
        this.resourceNames = resourceNames;
    }
}
