package com.tradernet.jpa.enums;

/**
 * Enumerates supported user statuses and their legacy integer identifiers.
 */
public enum UserStatus {
    STANDARD(0),
    SYSTEM(1),
    DELETED(2),
    DISABLED(3);

    private final int id;

    UserStatus(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static UserStatus get(int id) {
        for (UserStatus status : values()) {
            if (status.id == id) {
                return status;
            }
        }
        return STANDARD;
    }
}
