package com.tradernet.jpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.Locale;

/**
 * Entity representing an API resource/entity that can be protected by security roles.
 */
@Entity
@Table(name = "tblResources")
public class ResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String pathPrefix;

    @Column(nullable = false, length = 16)
    private String httpMethod = "*";

    @ManyToMany(mappedBy = "resources")
    private final Set<RoleEntity> roles = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod == null || httpMethod.isBlank()
            ? "*"
            : httpMethod.trim().toUpperCase(Locale.ROOT);
    }

    public Set<RoleEntity> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    void addRoleReference(RoleEntity role) {
        roles.add(role);
    }

    void removeRoleReference(RoleEntity role) {
        roles.remove(role);
    }
}
