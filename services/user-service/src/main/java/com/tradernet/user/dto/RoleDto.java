package com.tradernet.user.dto;

import java.util.Set;

/**
 * Role API payload including allowed resources.
 */
public class RoleDto {

    private String name;
    private Set<String> resourceNames;

    public RoleDto() {
    }

    public RoleDto(String name, Set<String> resourceNames) {
        this.name = name;
        this.resourceNames = resourceNames;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getResourceNames() {
        return resourceNames;
    }

    public void setResourceNames(Set<String> resourceNames) {
        this.resourceNames = resourceNames;
    }
}
