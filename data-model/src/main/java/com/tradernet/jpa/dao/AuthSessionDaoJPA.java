package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.AuthSessionEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.util.Optional;

/**
 * JPA implementation of authenticated-session persistence.
 */
@Stateless
public class AuthSessionDaoJPA implements AuthSessionDao {

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Override
    public void save(AuthSessionEntity session) {
        entityManager.persist(session);
    }

    @Override
    public Optional<AuthSessionEntity> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(entityManager.find(AuthSessionEntity.class, tokenHash));
    }

    @Override
    public void deleteByTokenHash(String tokenHash) {
        findByTokenHash(tokenHash).ifPresent(entityManager::remove);
    }

    @Override
    public void deleteByUserId(long userId) {
        entityManager.createQuery("DELETE FROM AuthSessionEntity s WHERE s.userId = :userId")
            .setParameter("userId", userId)
            .executeUpdate();
    }

    @Override
    public int deleteExpired(Instant now, Instant idleCutoff) {
        return entityManager.createQuery(
                "DELETE FROM AuthSessionEntity s "
                    + "WHERE s.expiresAt <= :now OR s.lastAccessedAt <= :idleCutoff"
            )
            .setParameter("now", now)
            .setParameter("idleCutoff", idleCutoff)
            .executeUpdate();
    }
}
