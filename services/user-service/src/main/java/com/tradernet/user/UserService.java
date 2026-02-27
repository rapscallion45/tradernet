package com.tradernet.user;

import com.tradernet.jpa.entities.UserEntity;
import jakarta.ejb.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing application users.
 * <p>
 * Provides methods for creating users, retrieving users by username,
 * and validating passwords. Uses JPA with Hibernate
 * and BCrypt for password hashing.
 */
@Singleton
public class UserService {


    /**
     * EntityManager instance for database operations.
     */
    @PersistenceContext(unitName = "tradernet")
    private EntityManager entityManager;

    /**
     * Finds a user by their username.
     *
     * @param username The username to search for
     * @return Optional containing the User if found, empty otherwise
     */
    public Optional<UserEntity> findByUsername(String username) {
        return entityManager.createNamedQuery("GetUserByUsername", UserEntity.class)
            .setParameter("username", username.toLowerCase())
            .getResultStream()
            .findFirst();
    }

    /**
     * Finds a user by username and eagerly loads roles to avoid lazy-loading issues
     * when accessed outside of an active persistence context.
     *
     * @param username The username to search for
     * @return Optional containing the User if found, empty otherwise
     */
    public Optional<UserEntity> findByUsernameWithRoles(String username) {
        return entityManager.createQuery(
                "select distinct u from UserEntity u " +
                    "left join fetch u.roles " +
                    "left join fetch u.groups " +
                    "left join fetch u.groups.roles " +
                    "left join fetch u.groups.parents " +
                    "left join fetch u.groups.parents.roles " +
                    "where lower(u.username) = :username",
                UserEntity.class
            )
            .setParameter("username", username.toLowerCase())
            .getResultStream()
            .findFirst();
    }

    /**
     * Finds all users and eagerly loads roles.
     *
     * @return List of users with roles loaded
     */
    public List<UserEntity> findAllWithRoles() {
        return entityManager.createQuery(
                "select distinct u from UserEntity u " +
                    "left join fetch u.roles " +
                    "left join fetch u.groups " +
                    "left join fetch u.groups.roles " +
                    "left join fetch u.groups.parents " +
                    "left join fetch u.groups.parents.roles " +
                    "order by u.username",
                UserEntity.class
            )
            .getResultList();
    }

    /**
     * Finds a user by id and eagerly loads roles.
     *
     * @param id User id
     * @return Optional containing the User if found, empty otherwise
     */
    public Optional<UserEntity> findByIdWithRoles(long id) {
        return entityManager.createQuery(
                "select distinct u from UserEntity u " +
                    "left join fetch u.roles " +
                    "left join fetch u.groups " +
                    "left join fetch u.groups.roles " +
                    "left join fetch u.groups.parents " +
                    "left join fetch u.groups.parents.roles " +
                    "where u.id = :id",
                UserEntity.class
            )
            .setParameter("id", id)
            .getResultStream()
            .findFirst();
    }

    /**
     * Creates a new user with the given username and password.
     * The password is hashed using BCrypt before storing in the database.
     *
     * @param username The username of the new user
     * @param password The plain-text password of the new user
     */
    public void createUser(String username, String password) {
        registerUser(username, password);
    }

    /**
     * Registers a new user and returns the created user record.
     *
     * @param username The username of the new user
     * @param password The plain-text password of the new user
     * @return The created user entity
     */
    public UserEntity registerUser(String username, String password) {
        if (findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("User already exists: " + username);
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        UserEntity user = new UserEntity(username);
        user.setPasswordHash(hashedPassword);
        entityManager.persist(user);
        return user;
    }

    /**
     * Validates that the provided password matches the stored password for the user.
     *
     * @param username The username of the user
     * @param password The plain-text password to validate
     * @return true if the password is correct, false otherwise
     */
    public boolean validatePassword(String username, String password) {
        return findByUsername(username)
            .map(user -> BCrypt.checkpw(password, user.getPasswordHash()))
            .orElse(false);
    }

    /**
     * Authenticates a user based on username and password credentials.
     *
     * @param username The user's username
     * @param password The plain-text password
     * @return true if authentication succeeds, false otherwise
     */
    public boolean authenticate(String username, String password) {
        return validatePassword(username, password);
    }

    /**
     * Resets a user's password (forgotten password flow).
     *
     * @param username    The username to reset
     * @param newPassword The new plain-text password
     */
    public void resetPassword(String username, String newPassword) {
        UserEntity user = findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPasswordHash(hashedPassword);
        entityManager.merge(user);
    }
}
