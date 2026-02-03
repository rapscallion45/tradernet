package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.RoleEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of RoleDao using Hibernate.
 */
@Stateless
public class RoleDaoJPA implements RoleDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(RoleEntity role) {
        entityManager.merge(role);
    }

    @Override
    public List<RoleEntity> findAll() {
        return entityManager.createQuery("SELECT r FROM RoleEntity r", RoleEntity.class)
            .getResultList();
    }

    @Override
    public Optional<RoleEntity> findByName(String name) {
        return entityManager.createQuery("SELECT r FROM RoleEntity r WHERE r.name = :name", RoleEntity.class)
            .setParameter("name", name)
            .getResultStream()
            .findFirst();
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM RoleEntity").executeUpdate();
    }
}
