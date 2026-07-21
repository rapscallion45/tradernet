package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.PasswordResetSessionEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.util.Locale;
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
        entityManager.persist(session);
    }

    @Override
    public Optional<PasswordResetSessionEntity> findByTokenHashForUpdate(String tokenHash) {
        return Optional.ofNullable(entityManager.find(
            PasswordResetSessionEntity.class,
            tokenHash,
            LockModeType.PESSIMISTIC_WRITE
        ));
    }

    @Override
    public void deleteByTokenHash(String tokenHash) {
        PasswordResetSessionEntity session = entityManager.find(PasswordResetSessionEntity.class, tokenHash);
        if (session != null) {
            entityManager.remove(session);
        }
    }

    @Override
    public void deleteByUsername(String username) {
        entityManager.createQuery("DELETE FROM PasswordResetSessionEntity s WHERE LOWER(s.username) = :username")
            .setParameter("username", username.toLowerCase(Locale.ROOT))
            .executeUpdate();
    }

    @Override
    public int deleteExpired(Instant now) {
        return entityManager.createQuery("DELETE FROM PasswordResetSessionEntity s WHERE s.expiresAt <= :now")
            .setParameter("now", now)
            .executeUpdate();
    }
}
