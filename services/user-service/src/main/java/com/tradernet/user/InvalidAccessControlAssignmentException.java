package com.tradernet.user;

import jakarta.ejb.ApplicationException;

/**
 * Indicates that a requested group, role, user, or resource assignment is invalid.
 */
@ApplicationException(rollback = true)
public class InvalidAccessControlAssignmentException extends RuntimeException {

    public InvalidAccessControlAssignmentException(String message) {
        super(message);
    }
}
