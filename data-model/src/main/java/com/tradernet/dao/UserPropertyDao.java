package com.tradernet.dao;

import com.tradernet.entities.UserPropertyEntity;

import java.util.List;

/**
 * Data Access Object (DAO) interface for user properties.
 */
public interface UserPropertyDao {

    /**
     * Saves a user property to the database.
     *
     * @param property property to save
     */
    void save(UserPropertyEntity property);

    /**
     * Retrieves all user properties.
     *
     * @return list of user properties
     */
    List<UserPropertyEntity> findAll();

    /**
     * Retrieves properties for a specific user.
     *
     * @param userId user identifier
     * @return list of properties for the user
     */
    List<UserPropertyEntity> findByUserId(long userId);

    /**
     * Deletes all user properties.
     */
    void deleteAll();
}
