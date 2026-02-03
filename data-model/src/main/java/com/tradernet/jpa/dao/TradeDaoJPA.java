package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.TradeEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/**
 * JPA implementation of TradeDao using Hibernate.
 */
@Stateless
public class TradeDaoJPA implements TradeDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(TradeEntity trade) {
        entityManager.persist(trade);
    }

    @Override
    public List<TradeEntity> findAll() {
        return entityManager.createQuery("SELECT t FROM TradeEntity t", TradeEntity.class)
            .getResultList();
    }

    @Override
    public List<TradeEntity> findBySymbol(String symbol) {
        return entityManager.createQuery("SELECT t FROM TradeEntity t WHERE t.symbol = :symbol", TradeEntity.class)
            .setParameter("symbol", symbol)
            .getResultList();
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM TradeEntity").executeUpdate();
    }
}
