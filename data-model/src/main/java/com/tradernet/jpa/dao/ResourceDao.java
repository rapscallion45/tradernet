package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.ResourceEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * DAO for protected resources/entities.
 */
public interface ResourceDao {

    ResourceEntity save(ResourceEntity resource);

    List<ResourceEntity> findAll();

    List<ResourceEntity> findMatchingWithRoles(Set<String> pathPrefixes, String httpMethod);

    Optional<ResourceEntity> findByName(String name);

    List<ResourceEntity> findByNames(Set<String> names);

    Optional<ResourceEntity> findByPathPrefix(String pathPrefix);
}
