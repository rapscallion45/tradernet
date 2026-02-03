package com.tradernet.jpa.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entity that stores a user-specific property value.
 */
@Entity
@Table(name = "tblUserProperties")
public class UserPropertyEntity {

    @EmbeddedId
    private UserPropertyId id = new UserPropertyId();

    private String value;

    public UserPropertyDefinition getPropDef() {
        return id.getPropDef();
    }

    public void setPropDef(UserPropertyDefinition propDef) {
        id.setPropDef(propDef);
    }

    public UserEntity getUser() {
        return id.getUser();
    }

    public void setUser(UserEntity user) {
        id.setUser(user);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
