package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.PasswordResetSessionEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.Instant;
import java.util.Optional;

/**
 * JPA implementation of password-reset-session persistence.
 */
@Stateless
public class PasswordResetSessionDaoJPA implements PasswordResetSessionDao {

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Override
    public void save(PasswordResetSessionEntity session) {
        if (!entityManager.contains(session)) {
            entityManager.persist(session);
        }
    }

    @Override
    public Optional<PasswordResetSessionEntity> findByUserIdForUpdate(long userId) {
        return Optional.ofNullable(entityManager.find(
            PasswordResetSessionEntity.class,
            userId,
            LockModeType.PESSIMISTIC_WRITE
        ));
    }

    @Override
    public Optional<PasswordResetSessionEntity> findByTokenHashForUpdate(String tokenHash) {
        return tokenQuery(tokenHash)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultStream()
            .findFirst();
    }

    @Override
    public Optional<PasswordResetSessionEntity> findByTokenHash(String tokenHash) {
        return tokenQuery(tokenHash).getResultStream().findFirst();
    }

    private TypedQuery<PasswordResetSessionEntity> tokenQuery(String tokenHash) {
        return entityManager.createQuery(
                "SELECT s FROM PasswordResetSessionEntity s WHERE s.tokenHash = :tokenHash",
                PasswordResetSessionEntity.class
            )
            .setParameter("tokenHash", tokenHash);
    }

    @Override
    public void deleteByUserId(long userId) {
        PasswordResetSessionEntity session = entityManager.find(PasswordResetSessionEntity.class, userId);
        if (session != null) {
            entityManager.remove(session);
        }
    }

    @Override
    public int deleteExpired(Instant now) {
        return entityManager.createQuery("DELETE FROM PasswordResetSessionEntity s WHERE s.expiresAt <= :now")
            .setParameter("now", now)
            .executeUpdate();
    }
}
