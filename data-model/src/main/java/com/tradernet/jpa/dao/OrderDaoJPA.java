package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.OrderEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of OrderDao using Hibernate.
 */
@Stateless
public class OrderDaoJPA implements OrderDao {

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Override
    public void save(OrderEntity order) {
        entityManager.persist(order);
    }

    @Override
    public List<OrderEntity> findAll() {
        return entityManager.createQuery("SELECT o FROM OrderEntity o ORDER BY o.createdAt DESC", OrderEntity.class)
            .getResultList();
    }

    @Override
    public List<OrderEntity> findByUserId(long userId) {
        return entityManager.createQuery("SELECT o FROM OrderEntity o WHERE o.userId = :userId ORDER BY o.createdAt DESC", OrderEntity.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    @Override
    public List<OrderEntity> findBySymbol(String symbol) {
        return entityManager.createQuery("SELECT o FROM OrderEntity o WHERE o.symbol = :symbol", OrderEntity.class)
            .setParameter("symbol", symbol)
            .getResultList();
    }

    @Override
    public Optional<OrderEntity> findById(long orderId) {
        return Optional.ofNullable(entityManager.find(OrderEntity.class, orderId));
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM OrderEntity").executeUpdate();
    }
}
