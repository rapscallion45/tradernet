package com.tradernet.user;

import jakarta.ejb.Local;

/**
 * Contract for security audit recording.
 */
@Local
public interface AuthenticationAudit {

    void record(String event, String outcome, String subject, String sourceAddress, String reason);
}
