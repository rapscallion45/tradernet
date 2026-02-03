package com.tradernet.entities;

import com.tradernet.databaseservice.passwords.PasswordHash;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    private PasswordHash password;

    public PasswordEntity() {
    }

    public PasswordEntity(UserEntity user, PasswordHash password) {
        this.user = user;
        this.password = password;
        this.lastChanged = new Date();
    }

    public PasswordEntity(PasswordEntity existingPassword, PasswordHash updatedPassword) {
        this.user = existingPassword.user;
        this.password = updatedPassword;
        this.lastChanged = new Date();
    }

    public Date getLastChanged() {
        return lastChanged;
    }

    public PasswordHash getPassword() {
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
    }
}
