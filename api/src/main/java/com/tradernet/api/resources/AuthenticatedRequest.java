package com.tradernet.api.resources;

import com.tradernet.user.dto.AuthUserDto;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;

final class AuthenticatedRequest {

    private static final String AUTHENTICATION_SCHEME = "TRADERNET_SESSION";

    private AuthenticatedRequest() {
    }

    static void setAuthenticatedUser(ContainerRequestContext requestContext, AuthUserDto authUser) {
        requestContext.setSecurityContext(new AuthenticatedUserSecurityContext(authUser, requestContext.getSecurityContext()));
    }

    static Optional<AuthUserDto> authenticatedUser(SecurityContext securityContext) {
        if (securityContext == null) {
            return Optional.empty();
        }

        Principal principal = securityContext.getUserPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal) {
            return Optional.of(((AuthenticatedUserPrincipal) principal).getAuthUser());
        }
        return Optional.empty();
    }

    static AuthUserDto requireAuthenticatedUser(SecurityContext securityContext) {
        return authenticatedUser(securityContext)
            .orElseThrow(NotAuthenticatedException::new);
    }

    private static final class AuthenticatedUserPrincipal implements Principal {
        private final AuthUserDto authUser;

        private AuthenticatedUserPrincipal(AuthUserDto authUser) {
            this.authUser = authUser;
        }

        @Override
        public String getName() {
            String username = authUser.getUsername();
            return username == null || username.isBlank() ? String.valueOf(authUser.getId()) : username;
        }

        private AuthUserDto getAuthUser() {
            return authUser;
        }
    }

    private static final class AuthenticatedUserSecurityContext implements SecurityContext {
        private final AuthenticatedUserPrincipal principal;
        private final SecurityContext delegate;

        private AuthenticatedUserSecurityContext(AuthUserDto authUser, SecurityContext delegate) {
            this.principal = new AuthenticatedUserPrincipal(authUser);
            this.delegate = delegate;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public boolean isUserInRole(String role) {
            if (role == null) {
                return false;
            }

            Set<String> roleNames = principal.getAuthUser().getRoleNames();
            return roleNames != null && roleNames.contains(role);
        }

        @Override
        public boolean isSecure() {
            return delegate != null && delegate.isSecure();
        }

        @Override
        public String getAuthenticationScheme() {
            String delegateScheme = delegate == null ? null : delegate.getAuthenticationScheme();
            if (delegateScheme != null && !delegateScheme.isBlank()) {
                return delegateScheme;
            }
            return AUTHENTICATION_SCHEME;
        }
    }
}
