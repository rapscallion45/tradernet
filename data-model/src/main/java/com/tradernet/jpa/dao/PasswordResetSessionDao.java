package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.PasswordResetSessionEntity;

import java.time.Instant;
import java.util.Optional;

/**
 * Persistence operations for password-reset token hashes.
 */
public interface PasswordResetSessionDao {

    void save(PasswordResetSessionEntity session);

    Optional<PasswordResetSessionEntity> findByTokenHashForUpdate(String tokenHash);

    void deleteByTokenHash(String tokenHash);

    void deleteByUsername(String username);

    int deleteExpired(Instant now);
}
