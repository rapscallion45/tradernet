package com.tradernet.dao;

import com.tradernet.entities.RoleEntity;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) interface for roles.
 */
public interface RoleDao {

    /**
     * Saves a role to the database.
     *
     * @param role role to save
     */
    void save(RoleEntity role);

    /**
     * Retrieves all roles.
     *
     * @return list of roles
     */
    List<RoleEntity> findAll();

    /**
     * Retrieves a role by name.
     *
     * @param name role name
     * @return role if found
     */
    Optional<RoleEntity> findByName(String name);

    /**
     * Deletes all roles.
     */
    void deleteAll();
}
