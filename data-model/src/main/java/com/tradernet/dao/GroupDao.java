package com.tradernet.dao;

import com.tradernet.entities.GroupEntity;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) interface for groups.
 */
public interface GroupDao {

    /**
     * Saves a group to the database.
     *
     * @param group group to save
     */
    void save(GroupEntity group);

    /**
     * Retrieves all groups.
     *
     * @return list of groups
     */
    List<GroupEntity> findAll();

    /**
     * Retrieves a group by id.
     *
     * @param id group identifier
     * @return group if found
     */
    Optional<GroupEntity> findById(long id);

    /**
     * Deletes all groups.
     */
    void deleteAll();
}
