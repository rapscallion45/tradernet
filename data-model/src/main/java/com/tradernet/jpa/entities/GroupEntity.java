package com.tradernet.jpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.tradernet.jpa.entities.util.RelationshipUpdateUtil.updateRelationship;

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

    @ManyToMany
    @JoinTable(
        name = "tblGroupRoles",
        joinColumns = @JoinColumn(name = "groupId"),
        inverseJoinColumns = @JoinColumn(name = "roleId")
    )
    private final Set<RoleEntity> roles = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "tblGroupParents",
        joinColumns = @JoinColumn(name = "groupId"),
        inverseJoinColumns = @JoinColumn(name = "parentGroupId")
    )
    private final Set<GroupEntity> parents = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    void addUserReference(UserEntity user) {
        users.add(user);
    }

    void removeUserReference(UserEntity user) {
        users.remove(user);
    }

    public Set<UserEntity> getUsers() {
        return Collections.unmodifiableSet(users);
    }

    public void setUsers(Set<UserEntity> newUsers) {
        updateRelationship(this.users, newUsers, this::addUser, this::removeUser);
    }

    public void clearUsers() {
        setUsers(Collections.emptySet());
    }

    public void addUser(UserEntity user) {
        addUserReference(user);
        user.addGroupReference(this);
    }

    public void removeUser(UserEntity user) {
        removeUserReference(user);
        user.removeGroupReference(this);
    }

    public Set<RoleEntity> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public void setRoles(Set<RoleEntity> newRoles) {
        updateRelationship(this.roles, newRoles, this::addRole, this::removeRole);
    }

    public void clearRoles() {
        setRoles(Collections.emptySet());
    }

    public void addRole(RoleEntity role) {
        addRoleReference(role);
        role.addGroupReference(this);
    }

    public void removeRole(RoleEntity role) {
        removeRoleReference(role);
        role.removeGroupReference(this);
    }

    void addRoleReference(RoleEntity role) {
        roles.add(role);
    }

    void removeRoleReference(RoleEntity role) {
        roles.remove(role);
    }

    public Set<GroupEntity> getAllParents() {
        return Set.copyOf(parents);
    }
}
