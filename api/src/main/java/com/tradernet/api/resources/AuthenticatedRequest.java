package com.tradernet.api.resources;

import com.tradernet.user.dto.AuthUserDto;
import jakarta.ws.rs.container.ContainerRequestContext;

import java.util.Optional;

final class AuthenticatedRequest {

    static final String AUTH_USER_PROPERTY = "tradernet.authUser";

    private AuthenticatedRequest() {
    }

    static void setAuthenticatedUser(ContainerRequestContext requestContext, AuthUserDto authUser) {
        requestContext.setProperty(AUTH_USER_PROPERTY, authUser);
    }

    static Optional<AuthUserDto> authenticatedUser(ContainerRequestContext requestContext) {
        Object value = requestContext.getProperty(AUTH_USER_PROPERTY);
        return toAuthUser(value);
    }

    private static Optional<AuthUserDto> toAuthUser(Object value) {
        if (value instanceof AuthUserDto) {
            return Optional.of((AuthUserDto) value);
        }
        return Optional.empty();
    }
}
