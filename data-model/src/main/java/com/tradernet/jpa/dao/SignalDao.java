package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.SignalEntity;

import java.util.List;

/**
 * Data Access Object (DAO) interface for signals.
 */
public interface SignalDao {

    /**
     * Saves a signal to the database.
     *
     * @param signal signal to save
     */
    void save(SignalEntity signal);

    /**
     * Retrieves all signals.
     *
     * @return list of signals
     */
    List<SignalEntity> findAll();

    /**
     * Retrieves all signals for a specific symbol.
     *
     * @param symbol trading symbol
     * @return list of signals for the symbol
     */
    List<SignalEntity> findBySymbol(String symbol);

    /**
     * Deletes all signals.
     */
    void deleteAll();
}
