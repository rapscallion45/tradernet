package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.AuthSessionEntity;

import java.time.Instant;
import java.util.Optional;

/**
 * Persistence operations for authenticated-session token hashes.
 */
public interface AuthSessionDao {

    void save(AuthSessionEntity session);

    Optional<AuthSessionEntity> findByTokenHash(String tokenHash);

    void deleteByTokenHash(String tokenHash);

    void deleteByUserId(long userId);

    int deleteExpired(Instant now);
}
