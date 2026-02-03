package com.tradernet.dao;

import com.tradernet.entities.PasswordEntity;

import java.util.List;

/**
 * Data Access Object (DAO) interface for user passwords.
 */
public interface PasswordDao {

    /**
     * Saves a password record to the database.
     *
     * @param password password record to save
     */
    void save(PasswordEntity password);

    /**
     * Retrieves all password records.
     *
     * @return list of password records
     */
    List<PasswordEntity> findAll();

    /**
     * Retrieves all password records for a user.
     *
     * @param userId user identifier
     * @return list of password records
     */
    List<PasswordEntity> findByUserId(long userId);

    /**
     * Deletes all password records.
     */
    void deleteAll();
}
