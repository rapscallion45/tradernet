package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.RoleEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Data Access Object (DAO) interface for roles.
 */
public interface RoleDao {

    /**
     * Saves a role to the database.
     *
     * @param role role to save
     */
    RoleEntity save(RoleEntity role);

    /**
     * Retrieves all roles.
     *
     * @return list of roles
     */
    List<RoleEntity> findAll();

    /**
     * Retrieves all roles with resources eagerly loaded.
     */
    List<RoleEntity> findAllWithResources();

    /**
     * Retrieves a role by name with resources eagerly loaded.
     *
     * @param name role name
     * @return role if found
     */
    Optional<RoleEntity> findByNameWithResources(String name);

    Optional<RoleEntity> findByNameWithResourcesForUpdate(String name);

    /**
     * Retrieves a role by name.
     *
     * @param name role name
     * @return role if found
     */
    Optional<RoleEntity> findByName(String name);

    List<RoleEntity> findByNames(Set<String> names);

    /**
     * Deletes all roles.
     */
    void deleteAll();
}
