package com.tradernet.api.resources;

import com.tradernet.user.dto.AuthUserDto;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedRequestTest {

    @Test
    void applicationRolesDoNotFallBackToContainerRoles() {
        final AtomicReference<SecurityContext> securityContext = new AtomicReference<>(containerSecurityContext());
        final ContainerRequestContext requestContext = (ContainerRequestContext) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{ContainerRequestContext.class},
            (proxy, method, arguments) -> {
                if ("getSecurityContext".equals(method.getName())) {
                    return securityContext.get();
                }
                if ("setSecurityContext".equals(method.getName())) {
                    securityContext.set((SecurityContext) arguments[0]);
                    return null;
                }
                return null;
            }
        );

        AuthenticatedRequest.setAuthenticatedUser(
            requestContext,
            new AuthUserDto(1L, "alice", Set.of("Application Role"))
        );

        assertTrue(securityContext.get().isUserInRole("Application Role"));
        assertFalse(securityContext.get().isUserInRole("Container Role"));
    }

    private SecurityContext containerSecurityContext() {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> "container-user";
            }

            @Override
            public boolean isUserInRole(String role) {
                return "Container Role".equals(role);
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public String getAuthenticationScheme() {
                return "CONTAINER";
            }
        };
    }
}
