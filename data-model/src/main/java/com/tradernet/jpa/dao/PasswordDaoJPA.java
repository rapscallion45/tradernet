package com.tradernet.jpa.dao;

import com.tradernet.dao.PasswordDao;
import com.tradernet.entities.PasswordEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/**
 * JPA implementation of PasswordDao using Hibernate.
 */
@Stateless
public class PasswordDaoJPA implements PasswordDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(PasswordEntity password) {
        entityManager.merge(password);
    }

    @Override
    public List<PasswordEntity> findAll() {
        return entityManager.createQuery("SELECT p FROM PasswordEntity p", PasswordEntity.class)
            .getResultList();
    }

    @Override
    public List<PasswordEntity> findByUserId(long userId) {
        return entityManager.createQuery("SELECT p FROM PasswordEntity p WHERE p.user.id = :userId", PasswordEntity.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM PasswordEntity").executeUpdate();
    }
}
