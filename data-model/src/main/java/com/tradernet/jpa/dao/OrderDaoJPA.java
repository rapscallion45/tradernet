package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.OrderEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/**
 * JPA implementation of OrderDao using Hibernate.
 */
@Stateless
public class OrderDaoJPA implements OrderDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(OrderEntity order) {
        entityManager.persist(order);
    }

    @Override
    public List<OrderEntity> findAll() {
        return entityManager.createQuery("SELECT o FROM OrderEntity o", OrderEntity.class)
            .getResultList();
    }

    @Override
    public List<OrderEntity> findBySymbol(String symbol) {
        return entityManager.createQuery("SELECT o FROM OrderEntity o WHERE o.symbol = :symbol", OrderEntity.class)
            .setParameter("symbol", symbol)
            .getResultList();
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM OrderEntity").executeUpdate();
    }
}
