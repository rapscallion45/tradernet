package com.tradernet.user;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emits sanitized, machine-searchable authentication security events.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AuthenticationAuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("com.tradernet.security.audit");

    public void record(String event, String outcome, String subject, String sourceAddress, String reason) {
        AUDIT.info(
            "event={} outcome={} subject={} source={} reason={}",
            sanitize(event, 40),
            sanitize(outcome, 24),
            sanitize(subject, 100),
            sanitize(sourceAddress, 64),
            sanitize(reason, 80)
        );
    }

    private String sanitize(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        final StringBuilder normalized = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index += 1) {
            final char character = value.charAt(index);
            normalized.append(Character.isWhitespace(character) || character == '=' ? '_' : character);
        }
        final String sanitized = normalized.toString();
        return sanitized.length() <= maximumLength
            ? sanitized
            : sanitized.substring(0, maximumLength);
    }
}
