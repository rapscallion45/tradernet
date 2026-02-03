package com.tradernet.common;

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
