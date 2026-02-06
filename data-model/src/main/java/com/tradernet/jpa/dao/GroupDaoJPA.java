package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.GroupEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of GroupDao using Hibernate.
 */
@Stateless
public class GroupDaoJPA implements GroupDao {

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Override
    public void save(GroupEntity group) {
        entityManager.merge(group);
    }

    @Override
    public List<GroupEntity> findAll() {
        return entityManager.createQuery("SELECT g FROM GroupEntity g", GroupEntity.class)
            .getResultList();
    }

    @Override
    public Optional<GroupEntity> findById(long id) {
        return Optional.ofNullable(entityManager.find(GroupEntity.class, id));
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM GroupEntity").executeUpdate();
    }
}
