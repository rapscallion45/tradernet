package com.tradernet.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Payload for updating allowed resources on a role.
 */
public class UpdateRoleRequestDto {

    @NotNull(message = "resourceNames is required")
    private Set<@NotBlank(message = "Resource name is required") String> resourceNames;

    public Set<String> getResourceNames() {
        return resourceNames;
    }

    public void setResourceNames(Set<String> resourceNames) {
        this.resourceNames = resourceNames;
    }
}
