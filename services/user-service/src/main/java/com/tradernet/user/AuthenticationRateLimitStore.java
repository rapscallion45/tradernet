package com.tradernet.user;

import com.tradernet.jpa.dao.AuthenticationRateLimitDao;
import com.tradernet.jpa.entities.AuthenticationRateLimitEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.Duration;
import java.time.Instant;

/**
 * Applies one rate-limit operation in an independent transaction.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class AuthenticationRateLimitStore {

    @EJB
    private AuthenticationRateLimitDao rateLimitDao;

    public AuthenticationRateLimitStore() {
    }

    AuthenticationRateLimitStore(AuthenticationRateLimitDao rateLimitDao) {
        this.rateLimitDao = rateLimitDao;
    }

    public RateLimitDecision consume(
        String bucketKey,
        int maximumAttempts,
        Duration window,
        Duration blockDuration,
        Instant now
    ) {
        final AuthenticationRateLimitEntity bucket = rateLimitDao.findForUpdate(bucketKey)
            .orElseGet(() -> newBucket(bucketKey, now, window));

        if (bucket.getBlockedUntil() != null && bucket.getBlockedUntil().isAfter(now)) {
            return RateLimitDecision.denied(secondsUntil(now, bucket.getBlockedUntil()));
        }

        if (!bucket.getWindowStartedAt().plus(window).isAfter(now)) {
            bucket.setWindowStartedAt(now);
            bucket.setAttemptCount(0);
            bucket.setBlockedUntil(null);
        }

        bucket.setAttemptCount(bucket.getAttemptCount() + 1);
        if (bucket.getAttemptCount() > maximumAttempts) {
            final Instant blockedUntil = now.plus(blockDuration);
            bucket.setBlockedUntil(blockedUntil);
            bucket.setExpiresAt(blockedUntil.plus(window));
            rateLimitDao.save(bucket);
            return RateLimitDecision.denied(secondsUntil(now, blockedUntil));
        }

        bucket.setExpiresAt(bucket.getWindowStartedAt().plus(window).plus(blockDuration));
        rateLimitDao.save(bucket);
        return RateLimitDecision.allowed();
    }

    private AuthenticationRateLimitEntity newBucket(String bucketKey, Instant now, Duration window) {
        final AuthenticationRateLimitEntity bucket = new AuthenticationRateLimitEntity();
        bucket.setBucketKey(bucketKey);
        bucket.setWindowStartedAt(now);
        bucket.setAttemptCount(0);
        bucket.setExpiresAt(now.plus(window));
        return bucket;
    }

    private long secondsUntil(Instant now, Instant future) {
        return Math.max(1, Duration.between(now, future).getSeconds());
    }
}
