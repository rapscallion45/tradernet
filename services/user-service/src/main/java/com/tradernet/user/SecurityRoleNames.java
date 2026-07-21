package com.tradernet.user;

import com.tradernet.user.dto.AuthUserDto;

/**
 * Canonical names for the built-in application security roles.
 */
public final class SecurityRoleNames {

    public static final String ALL_RIGHTS = "ALL Rights";
    public static final String ADMIN_RIGHTS = "Admin Rights";
    public static final String STANDARD_RIGHTS = "Standard Rights";

    private SecurityRoleNames() {
    }

    public static boolean hasRole(AuthUserDto user, String roleName) {
        return user != null
            && user.getRoleNames() != null
            && user.getRoleNames().contains(roleName);
    }
}
