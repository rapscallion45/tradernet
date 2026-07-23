package com.tradernet.api.resources;

import com.tradernet.user.ResourcePathNormalizer;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Prevents browsers and intermediaries from caching authentication responses.
 */
@Provider
public class AuthenticationResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        final String path = ResourcePathNormalizer.normalize(requestContext.getUriInfo().getPath());
        if (path.equals("auth") || path.startsWith("auth/")) {
            responseContext.getHeaders().putSingle("Cache-Control", "no-store");
            responseContext.getHeaders().putSingle("Pragma", "no-cache");
        }
    }
}
