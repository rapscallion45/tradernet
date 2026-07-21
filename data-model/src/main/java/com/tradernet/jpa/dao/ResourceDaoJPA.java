package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.ResourceEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * JPA implementation of resource DAO.
 */
@Stateless
public class ResourceDaoJPA implements ResourceDao {

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Override
    public ResourceEntity save(ResourceEntity resource) {
        if (resource.getId() == null) {
            entityManager.persist(resource);
            return resource;
        }
        return entityManager.merge(resource);
    }

    @Override
    public List<ResourceEntity> findAll() {
        return entityManager.createQuery("SELECT r FROM ResourceEntity r", ResourceEntity.class).getResultList();
    }

    @Override
    public List<ResourceEntity> findMatchingWithRoles(Set<String> pathPrefixes, String httpMethod) {
        if (pathPrefixes == null || pathPrefixes.isEmpty()) {
            return List.of();
        }

        return entityManager.createQuery(
                "SELECT DISTINCT r FROM ResourceEntity r "
                    + "LEFT JOIN FETCH r.roles "
                    + "WHERE r.pathPrefix IN :pathPrefixes "
                    + "AND (r.httpMethod IS NULL OR TRIM(r.httpMethod) = '' OR TRIM(r.httpMethod) = '*' "
                    + "OR UPPER(TRIM(r.httpMethod)) = :httpMethod)",
                ResourceEntity.class
            )
            .setParameter("pathPrefixes", pathPrefixes)
            .setParameter("httpMethod", normalizeMethod(httpMethod))
            .getResultList();
    }

    @Override
    public Optional<ResourceEntity> findByName(String name) {
        return entityManager.createQuery("SELECT r FROM ResourceEntity r WHERE r.name = :name", ResourceEntity.class)
            .setParameter("name", name)
            .getResultStream()
            .findFirst();
    }

    @Override
    public List<ResourceEntity> findByNames(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("SELECT r FROM ResourceEntity r WHERE r.name IN :names", ResourceEntity.class)
            .setParameter("names", names)
            .getResultList();
    }

    @Override
    public Optional<ResourceEntity> findByPathPrefix(String pathPrefix) {
        return entityManager.createQuery("SELECT r FROM ResourceEntity r WHERE r.pathPrefix = :pathPrefix", ResourceEntity.class)
            .setParameter("pathPrefix", pathPrefix)
            .getResultStream()
            .findFirst();
    }

    private String normalizeMethod(String httpMethod) {
        return httpMethod == null ? "" : httpMethod.trim().toUpperCase(Locale.ROOT);
    }
}
