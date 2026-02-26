package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.ResourceEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of resource DAO.
 */
@Stateless
public class ResourceDaoJPA implements ResourceDao {

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Override
    public void save(ResourceEntity resource) {
        entityManager.merge(resource);
    }

    @Override
    public List<ResourceEntity> findAll() {
        return entityManager.createQuery("SELECT r FROM ResourceEntity r", ResourceEntity.class).getResultList();
    }

    @Override
    public List<ResourceEntity> findAllWithRoles() {
        return entityManager.createQuery("SELECT DISTINCT r FROM ResourceEntity r LEFT JOIN FETCH r.roles", ResourceEntity.class).getResultList();
    }

    @Override
    public Optional<ResourceEntity> findByName(String name) {
        return entityManager.createQuery("SELECT r FROM ResourceEntity r WHERE r.name = :name", ResourceEntity.class)
            .setParameter("name", name)
            .getResultStream()
            .findFirst();
    }

    @Override
    public Optional<ResourceEntity> findByPathPrefix(String pathPrefix) {
        return entityManager.createQuery("SELECT r FROM ResourceEntity r WHERE r.pathPrefix = :pathPrefix", ResourceEntity.class)
            .setParameter("pathPrefix", pathPrefix)
            .getResultStream()
            .findFirst();
    }
}
