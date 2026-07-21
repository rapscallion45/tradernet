package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.UserEntity;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) interface for users.
 */
public interface UserDao {

    /**
     * Saves a user to the database.
     *
     * @param user user to save
     */
    UserEntity save(UserEntity user);

    /**
     * Retrieves all users.
     *
     * @return list of users
     */
    List<UserEntity> findAll();

    /**
     * Retrieves a user by id.
     *
     * @param id user identifier
     * @return user if found
     */
    Optional<UserEntity> findById(long id);

    /**
     * Retrieves a user by username.
     *
     * @param username username to search
     * @return user if found
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * Retrieves all users with their effective role relationships initialized.
     *
     * @return users ordered by username
     */
    List<UserEntity> findAllWithRoles();

    /**
     * Retrieves a user by id with their effective role relationships initialized.
     *
     * @param id user identifier
     * @return user if found
     */
    Optional<UserEntity> findByIdWithRoles(long id);

    /**
     * Retrieves a user by username with their effective role relationships initialized.
     *
     * @param username username to search
     * @return user if found
     */
    Optional<UserEntity> findByUsernameWithRoles(String username);

    /**
     * Deletes all users.
     */
    void deleteAll();
}
