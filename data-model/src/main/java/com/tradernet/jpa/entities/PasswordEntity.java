package com.tradernet.jpa.entities;

import com.tradernet.jpa.databaseservice.passwords.PasswordHash;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Date;

/**
 * Entity representing a stored password hash for a user.
 */
@Entity
@Table(name = "tblPasswords")
public class PasswordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userId")
    private UserEntity user;

    private Date lastChanged;

    @Transient
    private PasswordHash password;

    private String passwordHash;

    public PasswordEntity() {
    }

    public PasswordEntity(UserEntity user, PasswordHash password) {
        this.user = user;
        this.password = password;
        this.passwordHash = password == null ? null : password.toString();
        this.lastChanged = new Date();
    }

    public PasswordEntity(PasswordEntity existingPassword, PasswordHash updatedPassword) {
        this.user = existingPassword.user;
        this.password = updatedPassword;
        this.passwordHash = updatedPassword == null ? null : updatedPassword.toString();
        this.lastChanged = new Date();
    }

    public Date getLastChanged() {
        return lastChanged;
    }

    public PasswordHash getPassword() {
        if (password == null && passwordHash != null) {
            password = new com.tradernet.jpa.databaseservice.passwords.PBKDF2PasswordHash(passwordHash);
        }
        return password;
    }

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public void setLastChanged(Date lastChanged) {
        this.lastChanged = lastChanged;
    }

    public void setPassword(PasswordHash password) {
        this.password = password;
        this.passwordHash = password == null ? null : password.toString();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.password = passwordHash == null
            ? null
            : new com.tradernet.jpa.databaseservice.passwords.PBKDF2PasswordHash(passwordHash);
    }
}
