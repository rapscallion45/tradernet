package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.UserPropertyEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/**
 * JPA implementation of UserPropertyDao using Hibernate.
 */
@Stateless
public class UserPropertyDaoJPA implements UserPropertyDao {

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Override
    public void save(UserPropertyEntity property) {
        entityManager.merge(property);
    }

    @Override
    public List<UserPropertyEntity> findAll() {
        return entityManager.createQuery("SELECT p FROM UserPropertyEntity p", UserPropertyEntity.class)
            .getResultList();
    }

    @Override
    public List<UserPropertyEntity> findByUserId(long userId) {
        return entityManager.createQuery("SELECT p FROM UserPropertyEntity p WHERE p.id.user.id = :userId", UserPropertyEntity.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM UserPropertyEntity").executeUpdate();
    }
}
