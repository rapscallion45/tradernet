package com.tradernet.dao;

import com.tradernet.entities.TradeEntity;

import java.util.List;

/**
 * Data Access Object (DAO) interface for trades.
 */
public interface TradeDao {

    /**
     * Saves a trade to the database.
     *
     * @param trade trade to save
     */
    void save(TradeEntity trade);

    /**
     * Retrieves all trades.
     *
     * @return list of trades
     */
    List<TradeEntity> findAll();

    /**
     * Retrieves trades for a specific symbol.
     *
     * @param symbol trading symbol
     * @return list of trades for the symbol
     */
    List<TradeEntity> findBySymbol(String symbol);

    /**
     * Deletes all trades.
     */
    void deleteAll();
}
