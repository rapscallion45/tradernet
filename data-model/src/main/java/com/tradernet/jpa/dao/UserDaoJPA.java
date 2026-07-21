package com.tradernet.jpa.dao;

import com.tradernet.jpa.entities.UserEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * JPA implementation of UserDao using Hibernate.
 */
@Stateless
public class UserDaoJPA implements UserDao {

    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    @Override
    public UserEntity save(UserEntity user) {
        if (user.getPk() == 0L) {
            entityManager.persist(user);
            return user;
        }
        return entityManager.merge(user);
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
            .setParameter("username", username.toLowerCase(Locale.ROOT))
            .getResultStream()
            .findFirst();
    }

    @Override
    public List<UserEntity> findAllWithRoles() {
        return entityManager.createQuery(
                "select distinct u from UserEntity u "
                    + "left join fetch u.roles "
                    + "left join fetch u.groups "
                    + "left join fetch u.groups.roles "
                    + "left join fetch u.groups.parents "
                    + "left join fetch u.groups.parents.roles "
                    + "order by u.username",
                UserEntity.class
            )
            .getResultList();
    }

    @Override
    public Optional<UserEntity> findByIdWithRoles(long id) {
        return entityManager.createQuery(
                "select distinct u from UserEntity u "
                    + "left join fetch u.roles "
                    + "left join fetch u.groups "
                    + "left join fetch u.groups.roles "
                    + "left join fetch u.groups.parents "
                    + "left join fetch u.groups.parents.roles "
                    + "where u.id = :id",
                UserEntity.class
            )
            .setParameter("id", id)
            .getResultStream()
            .findFirst();
    }

    @Override
    public Optional<UserEntity> findByUsernameWithRoles(String username) {
        return entityManager.createQuery(
                "select distinct u from UserEntity u "
                    + "left join fetch u.roles "
                    + "left join fetch u.groups "
                    + "left join fetch u.groups.roles "
                    + "left join fetch u.groups.parents "
                    + "left join fetch u.groups.parents.roles "
                    + "where lower(u.username) = :username",
                UserEntity.class
            )
            .setParameter("username", username.toLowerCase(Locale.ROOT))
            .getResultStream()
            .findFirst();
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM UserEntity").executeUpdate();
    }
}
