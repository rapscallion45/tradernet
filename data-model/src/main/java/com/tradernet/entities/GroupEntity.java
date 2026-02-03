package com.tradernet.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a logical group of users with parent relationships.
 */
@Entity
@Table(name = "tblGroups")
public class GroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(name = "tblGroupUsers")
    private final Set<UserEntity> users = new HashSet<>();

    private final Set<GroupEntity> parents = new HashSet<>();

    void addUserReference(UserEntity user) {
        users.add(user);
    }

    void removeUserReference(UserEntity user) {
        users.remove(user);
    }

    public Set<GroupEntity> getAllParents() {
        return Set.copyOf(parents);
    }
}
