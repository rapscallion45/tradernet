package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.AuthenticationRateLimitEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.util.Optional;

/**
 * JPA implementation of authentication rate-limit persistence.
 */
@Stateless
public class AuthenticationRateLimitDaoJPA implements AuthenticationRateLimitDao {

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Override
    public Optional<AuthenticationRateLimitEntity> findForUpdate(String bucketKey) {
        return Optional.ofNullable(entityManager.find(
            AuthenticationRateLimitEntity.class,
            bucketKey,
            LockModeType.PESSIMISTIC_WRITE
        ));
    }

    @Override
    public void save(AuthenticationRateLimitEntity bucket) {
        if (entityManager.contains(bucket)) {
            return;
        }
        entityManager.persist(bucket);
        entityManager.flush();
    }

    @Override
    public int deleteExpired(Instant now) {
        return entityManager.createQuery(
                "DELETE FROM AuthenticationRateLimitEntity b WHERE b.expiresAt <= :now"
            )
            .setParameter("now", now)
            .executeUpdate();
    }
}
