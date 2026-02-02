package com.tradernet.user;

import com.tradernet.model.User;
import jakarta.ejb.Singleton;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * Service for managing application users.
 * <p>
 * Provides methods for creating users, retrieving users by username,
 * and validating passwords. Uses Spring JDBC with a provided DataSource
 * and BCrypt for password hashing.
 */
@Singleton
public class UserService {

    /**
     * JdbcTemplate instance for database operations.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructs a UserService with the given DataSource.
     *
     * @param ds The DataSource to use for database access
     */
    public UserService(DataSource ds) {
        this.jdbcTemplate = new JdbcTemplate(ds);
    }

    /**
     * Finds a user by their username.
     *
     * @param username The username to search for
     * @return Optional containing the User if found, empty otherwise
     */
    public Optional<User> findByUsername(String username) {
        return jdbcTemplate.query(
            "SELECT username, password_hash FROM users WHERE username = ?",
            rs -> {
                if (rs.next()) {
                    User user = new User();
                    user.setUsername(rs.getString("username"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    return Optional.of(user);
                }
                return Optional.empty();
            },
            username
        );
    }

    /**
     * Creates a new user with the given username and password.
     * The password is hashed using BCrypt before storing in the database.
     *
     * @param username The username of the new user
     * @param password The plain-text password of the new user
     */
    public void createUser(String username, String password) {
        // Hash the password using BCrypt with a generated salt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        jdbcTemplate.update(
            "INSERT INTO users(username, password_hash) VALUES (?, ?)",
            username,
            hashedPassword
        );
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
}
