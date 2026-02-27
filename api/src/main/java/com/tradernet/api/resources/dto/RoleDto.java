package com.tradernet.api.resources.dto;

import com.tradernet.jpa.entities.RoleEntity;

import java.util.Set;
import java.util.stream.Collectors;

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

    public static RoleDto fromEntity(RoleEntity role) {
        return new RoleDto(
            role.getName(),
            role.getResources().stream().map(resource -> resource.getName()).collect(Collectors.toSet())
        );
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
