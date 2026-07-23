package com.tradernet.user;

/**
 * Canonicalizes API resource paths before authentication and authorization checks.
 */
public final class ResourcePathNormalizer {

    private ResourcePathNormalizer() {
    }

    public static String normalize(String path) {
        if (path == null) {
            return "";
        }

        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        if (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        return normalizedPath;
    }
}
