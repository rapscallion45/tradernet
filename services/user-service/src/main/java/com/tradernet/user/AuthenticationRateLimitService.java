package com.tradernet.user;

import com.tradernet.jpa.dao.AuthenticationRateLimitDao;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

/**
 * Builds privacy-preserving source buckets for distributed authentication throttling.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AuthenticationRateLimitService {

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final int MAXIMUM_CONCURRENT_INSERT_ATTEMPTS = 3;

    @EJB
    private AuthenticationRateLimitStore store;

    @EJB
    private AuthenticationRateLimitDao rateLimitDao;

    @EJB
    private UserSecurityPolicy configuration;

    public RateLimitDecision checkLogin(String sourceAddress) {
        return consume(
            bucketKey("login", sourceAddress),
            configuration.getLoginRateLimitAttempts(),
            Instant.now()
        );
    }

    public RateLimitDecision checkPasswordReset(String sourceAddress) {
        return consume(
            bucketKey("reset", sourceAddress),
            configuration.getResetRateLimitAttempts(),
            Instant.now()
        );
    }

    @Schedule(hour = "*", minute = "37", second = "0", persistent = false)
    public void removeExpiredBuckets() {
        rateLimitDao.deleteExpired(Instant.now());
    }

    String bucketKey(String operation, String sourceAddress) {
        final String source = sourceAddress == null || sourceAddress.isBlank()
            ? "unknown"
            : sourceAddress.trim();
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8));
            return operation + ":" + toHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private RateLimitDecision consume(String bucketKey, int maximumAttempts, Instant now) {
        EJBException lastFailure = null;
        for (int attempt = 1; attempt <= MAXIMUM_CONCURRENT_INSERT_ATTEMPTS; attempt += 1) {
            try {
                return store.consume(
                    bucketKey,
                    maximumAttempts,
                    configuration.getRateLimitWindow(),
                    configuration.getRateLimitBlockDuration(),
                    now
                );
            } catch (EJBException ex) {
                lastFailure = ex;
            }
        }
        throw lastFailure;
    }

    private String toHex(byte[] bytes) {
        final char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i += 1) {
            final int value = bytes[i] & 0xff;
            result[i * 2] = HEX[value >>> 4];
            result[i * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(result);
    }
}
