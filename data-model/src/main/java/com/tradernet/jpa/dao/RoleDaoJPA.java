package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.RoleEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JPA implementation of RoleDao using Hibernate.
 */
@Stateless
public class RoleDaoJPA implements RoleDao {

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Override
    public RoleEntity save(RoleEntity role) {
        if (role.getId() == null) {
            entityManager.persist(role);
            return role;
        }
        return entityManager.merge(role);
    }

    @Override
    public List<RoleEntity> findAll() {
        return entityManager.createQuery("SELECT r FROM RoleEntity r", RoleEntity.class)
            .getResultList();
    }

    @Override
    public List<RoleEntity> findAllWithResources() {
        return entityManager.createQuery("SELECT DISTINCT r FROM RoleEntity r LEFT JOIN FETCH r.resources", RoleEntity.class)
            .getResultList();
    }

    @Override
    public Optional<RoleEntity> findByNameWithResources(String name) {
        return entityManager.createQuery(
                "SELECT DISTINCT r FROM RoleEntity r LEFT JOIN FETCH r.resources WHERE r.name = :name",
                RoleEntity.class
            )
            .setParameter("name", name)
            .getResultStream()
            .findFirst();
    }

    @Override
    public Optional<RoleEntity> findByNameWithResourcesForUpdate(String name) {
        final Optional<RoleEntity> locked = entityManager.createQuery(
                "SELECT r FROM RoleEntity r WHERE r.name = :name",
                RoleEntity.class
            )
            .setParameter("name", name)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultStream()
            .findFirst();
        return locked.isEmpty() ? Optional.empty() : findByNameWithResources(name);
    }

    @Override
    public Optional<RoleEntity> findByName(String name) {
        return entityManager.createQuery("SELECT r FROM RoleEntity r WHERE r.name = :name", RoleEntity.class)
            .setParameter("name", name)
            .getResultStream()
            .findFirst();
    }

    @Override
    public List<RoleEntity> findByNames(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("SELECT r FROM RoleEntity r WHERE r.name IN :names", RoleEntity.class)
            .setParameter("names", names)
            .getResultList();
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM RoleEntity").executeUpdate();
    }
}
