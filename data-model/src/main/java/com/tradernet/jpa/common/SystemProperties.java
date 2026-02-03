package com.tradernet.jpa.common;

/**
 * Utility class for accessing system configuration flags.
 */
public final class SystemProperties {

    private SystemProperties() {
    }

    public static String EfsExternalIdentityManagement() {
        return "efs.external.identity.management";
    }

    public static boolean getBooleanFlag(String key) {
        return Boolean.parseBoolean(System.getProperty(key, "false"));
    }
}
