package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.GroupEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.LockModeType;

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
    public GroupEntity save(GroupEntity group) {
        if (group.getId() == null) {
            entityManager.persist(group);
            return group;
        }
        return entityManager.merge(group);
    }

    @Override
    public List<GroupEntity> findAll() {
        return entityManager.createQuery("SELECT DISTINCT g FROM GroupEntity g LEFT JOIN FETCH g.users LEFT JOIN FETCH g.roles", GroupEntity.class)
            .getResultList();
    }

    @Override
    public Optional<GroupEntity> findById(long id) {
        return entityManager.createQuery(
                "SELECT DISTINCT g FROM GroupEntity g LEFT JOIN FETCH g.users LEFT JOIN FETCH g.roles WHERE g.id = :id",
                GroupEntity.class
            )
            .setParameter("id", id)
            .getResultStream()
            .findFirst();
    }

    @Override
    public Optional<GroupEntity> findByIdForUpdate(long id) {
        final GroupEntity locked = entityManager.find(GroupEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        return locked == null ? Optional.empty() : findById(id);
    }

    @Override
    public Optional<GroupEntity> findByName(String name) {
        return entityManager.createQuery(
                "SELECT DISTINCT g FROM GroupEntity g LEFT JOIN FETCH g.users LEFT JOIN FETCH g.roles WHERE g.name = :name",
                GroupEntity.class
            )
            .setParameter("name", name)
            .getResultStream()
            .findFirst();
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM GroupEntity").executeUpdate();
    }
}
