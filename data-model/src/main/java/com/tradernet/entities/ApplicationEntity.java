package com.tradernet.entities;

import jakarta.persistence.MappedSuperclass;
import java.util.HashSet;
import java.util.Set;

@MappedSuperclass
public abstract class ApplicationEntity {

    private final Set<UserEntity> users = new HashSet<>();

    public void addUser(UserEntity user) {
        users.add(user);
    }

    public void removeUser(UserEntity user) {
        users.remove(user);
    }
}
