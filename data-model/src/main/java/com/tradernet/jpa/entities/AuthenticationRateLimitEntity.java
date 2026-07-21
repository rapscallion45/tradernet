package com.tradernet.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Cluster-wide authentication rate-limit state for a hashed request source.
 */
@Entity
@Table(name = "tblAuthenticationRateLimits")
public class AuthenticationRateLimitEntity {

    @Id
    @Column(name = "bucket_key", length = 96)
    private String bucketKey;

    @Column(nullable = false)
    private Instant windowStartedAt;

    @Column(nullable = false)
    private int attemptCount;

    private Instant blockedUntil;

    @Column(nullable = false)
    private Instant expiresAt;

    public String getBucketKey() {
        return bucketKey;
    }

    public void setBucketKey(String bucketKey) {
        this.bucketKey = bucketKey;
    }

    public Instant getWindowStartedAt() {
        return windowStartedAt;
    }

    public void setWindowStartedAt(Instant windowStartedAt) {
        this.windowStartedAt = windowStartedAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getBlockedUntil() {
        return blockedUntil;
    }

    public void setBlockedUntil(Instant blockedUntil) {
        this.blockedUntil = blockedUntil;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
