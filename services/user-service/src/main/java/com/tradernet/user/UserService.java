package com.tradernet.user;

import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.user.dto.UserProfileDto;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing application users.
 * <p>
 * Provides methods for retrieving users by username or id,
 * and authenticating passwords. Uses JPA with Hibernate
 * and BCrypt for password hashing.
 */
@Stateless
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
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

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
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

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

    public List<UserProfileDto> getUserProfiles() {
        return findAllWithRoles().stream()
            .map(UserProfileDto::fromUser)
            .collect(Collectors.toList());
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

    public Optional<UserProfileDto> getUserProfile(long id) {
        return findByIdWithRoles(id).map(UserProfileDto::fromUser);
    }

    public Optional<UserProfileDto> getUserProfileByUsername(String username) {
        return findByUsernameWithRoles(username).map(UserProfileDto::fromUser);
    }

    public Optional<UserEntity> findAuthenticatedUser(String username, String password) {
        if (password == null || password.isBlank()) {
            return Optional.empty();
        }

        return findByUsernameWithRoles(username)
            .filter(user -> passwordMatches(user, password));
    }

    /**
     * Authenticates a user based on username and password credentials.
     *
     * @param username The user's username
     * @param password The plain-text password
     * @return true if authentication succeeds, false otherwise
     */
    public boolean authenticate(String username, String password) {
        if (password == null || password.isBlank()) {
            return false;
        }

        return findByUsername(username)
            .filter(user -> passwordMatches(user, password))
            .isPresent();
    }

    private boolean passwordMatches(UserEntity user, String password) {
        String passwordHash = user.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }

        try {
            return BCrypt.checkpw(password, passwordHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Resets a user's password (forgotten password flow).
     *
     * @param username    The username to reset
     * @param newPassword The new plain-text password
     */
    public void resetPassword(String username, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("newPassword is required");
        }

        UserEntity user = findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPasswordHash(hashedPassword);
        user.setChangePasswordNextLogin(false);
        entityManager.merge(user);
    }

}
