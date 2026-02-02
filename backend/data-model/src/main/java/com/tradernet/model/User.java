package com.tradernet.model;

/**
 * Represents an application user.
 * <p>
 * Contains basic user information and the password hash.
 * This class is used by UserService for authentication and storage.
 */
public class User {

    /**
     * The unique username of the user.
     */
    private String username;

    /**
     * The hashed password of the user (BCrypt hash).
     */
    private String passwordHash;

    /**
     * Default constructor.
     */
    public User() {
    }

    /**
     * Constructs a User with the given username and password hash.
     *
     * @param username     The user's username
     * @param passwordHash The hashed password
     */
    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    /**
     * Gets the username.
     *
     * @return The user's username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username The username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password hash.
     *
     * @return The hashed password
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the password hash.
     *
     * @param passwordHash The hashed password to set
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Returns a string representation of the User object.
     * For security, the password hash is included but not the plain password.
     *
     * @return String representation of the User
     */
    @Override
    public String toString() {
        return "User{" +
            "username='" + username + '\'' +
            ", passwordHash='" + passwordHash + '\'' +
            '}';
    }
}
