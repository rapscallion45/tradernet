package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.ResourceEntity;

import java.util.List;
import java.util.Optional;

/**
 * DAO for protected resources/entities.
 */
public interface ResourceDao {

    void save(ResourceEntity resource);

    List<ResourceEntity> findAll();

    List<ResourceEntity> findAllWithRoles();

    Optional<ResourceEntity> findByName(String name);

    Optional<ResourceEntity> findByPathPrefix(String pathPrefix);
}
