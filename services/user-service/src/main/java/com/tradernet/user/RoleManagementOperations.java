package com.tradernet.user;

import com.tradernet.user.dto.AuthUserDto;
import com.tradernet.user.dto.RoleDto;
import jakarta.ejb.Local;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Contract for role administration workflows.
 */
@Local
public interface RoleManagementOperations {

    List<RoleDto> getRoles();

    Optional<RoleDto> getRole(String name);

    List<String> getResourceNames();

    Optional<RoleDto> updateRole(String name, Set<String> resourceNames, AuthUserDto actor);
}
