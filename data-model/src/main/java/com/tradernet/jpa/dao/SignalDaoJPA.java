package com.tradernet.jpa.dao;

import com.tradernet.dao.SignalDao;
import com.tradernet.entities.SignalEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/**
 * JPA implementation of SignalDao using Hibernate.
 */
@Stateless
public class SignalDaoJPA implements SignalDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(SignalEntity signal) {
        entityManager.persist(signal);
    }

    @Override
    public List<SignalEntity> findAll() {
        return entityManager.createQuery("SELECT s FROM SignalEntity s", SignalEntity.class)
            .getResultList();
    }

    @Override
    public List<SignalEntity> findBySymbol(String symbol) {
        return entityManager.createQuery("SELECT s FROM SignalEntity s WHERE s.symbol = :symbol", SignalEntity.class)
            .setParameter("symbol", symbol)
            .getResultList();
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM SignalEntity").executeUpdate();
    }
}
