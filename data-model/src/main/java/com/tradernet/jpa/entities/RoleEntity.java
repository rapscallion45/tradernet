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
 * Entity representing a permission role that can be assigned to users.
 */
@Entity
@Table(name = "tblRoles")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "roles")
    private final Set<UserEntity> users = new HashSet<>();

    @ManyToMany(mappedBy = "roles")
    private final Set<GroupEntity> groups = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "tblRoleResources",
        joinColumns = @JoinColumn(name = "roleId"),
        inverseJoinColumns = @JoinColumn(name = "resourceId")
    )
    private final Set<ResourceEntity> resources = new HashSet<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    void addUserReference(UserEntity user) {
        users.add(user);
    }

    void removeUserReference(UserEntity user) {
        users.remove(user);
    }

    void addGroupReference(GroupEntity group) {
        groups.add(group);
    }

    void removeGroupReference(GroupEntity group) {
        groups.remove(group);
    }

    public Set<ResourceEntity> getResources() {
        return Collections.unmodifiableSet(resources);
    }

    public void setResources(Set<ResourceEntity> newResources) {
        updateRelationship(this.resources, newResources, this::addResource, this::removeResource);
    }

    public void addResource(ResourceEntity resource) {
        addResourceReference(resource);
        resource.addRoleReference(this);
    }

    public void removeResource(ResourceEntity resource) {
        removeResourceReference(resource);
        resource.removeRoleReference(this);
    }

    void addResourceReference(ResourceEntity resource) {
        resources.add(resource);
    }

    void removeResourceReference(ResourceEntity resource) {
        resources.remove(resource);
    }
}
