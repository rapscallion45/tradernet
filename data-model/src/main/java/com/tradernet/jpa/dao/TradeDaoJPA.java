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

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Override
    public void save(TradeEntity trade) {
        entityManager.persist(trade);
    }

    @Override
    public List<TradeEntity> findAll() {
        return entityManager.createQuery("SELECT t FROM TradeEntity t ORDER BY t.timestamp DESC", TradeEntity.class)
            .getResultList();
    }

    @Override
    public List<TradeEntity> findBySymbol(String symbol) {
        return entityManager.createQuery("SELECT t FROM TradeEntity t WHERE t.symbol = :symbol ORDER BY t.timestamp DESC", TradeEntity.class)
            .setParameter("symbol", symbol)
            .getResultList();
    }

    @Override
    public List<TradeEntity> findByUserId(long userId) {
        return entityManager.createQuery("SELECT t FROM TradeEntity t WHERE t.userId = :userId ORDER BY t.timestamp DESC", TradeEntity.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    @Override
    public List<TradeEntity> findByUserIdAndSymbol(long userId, String symbol) {
        return entityManager.createQuery(
                "SELECT t FROM TradeEntity t WHERE t.userId = :userId AND t.symbol = :symbol ORDER BY t.timestamp DESC",
                TradeEntity.class
            )
            .setParameter("userId", userId)
            .setParameter("symbol", symbol)
            .getResultList();
    }

    @Override
    public boolean existsByOrderIdAndExecutionType(long orderId, String executionType) {
        Long count = entityManager.createQuery(
                "SELECT COUNT(t) FROM TradeEntity t WHERE t.orderId = :orderId AND t.executionType = :executionType",
                Long.class
            )
            .setParameter("orderId", orderId)
            .setParameter("executionType", executionType)
            .getSingleResult();
        return count > 0;
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM TradeEntity").executeUpdate();
    }
}
