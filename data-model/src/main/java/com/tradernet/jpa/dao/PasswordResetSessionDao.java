package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.PasswordResetSessionEntity;

import java.time.Instant;
import java.util.Optional;

/**
 * Persistence operations for password-reset token hashes.
 */
public interface PasswordResetSessionDao {

    void save(PasswordResetSessionEntity session);

    Optional<PasswordResetSessionEntity> findByUserIdForUpdate(long userId);

    Optional<PasswordResetSessionEntity> findByTokenHash(String tokenHash);

    Optional<PasswordResetSessionEntity> findByTokenHashForUpdate(String tokenHash);

    void deleteByUserId(long userId);

    int deleteExpired(Instant now);
}
