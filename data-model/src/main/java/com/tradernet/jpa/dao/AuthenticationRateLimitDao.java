package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.AuthenticationRateLimitEntity;

import java.time.Instant;
import java.util.Optional;

/**
 * Persistence operations for cluster-wide authentication throttles.
 */
public interface AuthenticationRateLimitDao {

    Optional<AuthenticationRateLimitEntity> findForUpdate(String bucketKey);

    void save(AuthenticationRateLimitEntity bucket);

    int deleteExpired(Instant now);
}
