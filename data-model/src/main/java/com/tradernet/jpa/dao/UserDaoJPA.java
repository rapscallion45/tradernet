package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.UserEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of UserDao using Hibernate.
 */
@Stateless
public class UserDaoJPA implements UserDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(UserEntity user) {
        entityManager.merge(user);
    }

    @Override
    public List<UserEntity> findAll() {
        return entityManager.createQuery("SELECT u FROM UserEntity u", UserEntity.class)
            .getResultList();
    }

    @Override
    public Optional<UserEntity> findById(long id) {
        return Optional.ofNullable(entityManager.find(UserEntity.class, id));
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return entityManager.createNamedQuery("GetUserByUsername", UserEntity.class)
            .setParameter("username", username.toLowerCase())
            .getResultStream()
            .findFirst();
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM UserEntity").executeUpdate();
    }
}
