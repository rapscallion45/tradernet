package com.tradernet.user;

import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.Local;

import java.util.Set;

/**
 * Contract for API authorization policy evaluation.
 */
@Local
public interface AuthorizationOperations {

    Set<String> getRequiredRoles(String httpMethod, String path);

    boolean hasAnyRole(AuthUserDto authUser, Set<String> allowedRoles);

    boolean canReadOwnUserByUsername(String httpMethod, String path, AuthUserDto authUser);
}
