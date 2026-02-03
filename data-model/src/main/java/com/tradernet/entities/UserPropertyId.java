package com.tradernet.entities;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite identifier for user properties.
 */
@Embeddable
public class UserPropertyId implements Serializable {

    @ManyToOne
    @JoinColumn(name = "userId")
    private UserEntity user;

    private UserPropertyDefinition propDef;

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public UserPropertyDefinition getPropDef() {
        return propDef;
    }

    public void setPropDef(UserPropertyDefinition propDef) {
        this.propDef = propDef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserPropertyId)) {
            return false;
        }
        UserPropertyId that = (UserPropertyId) o;
        return Objects.equals(user, that.user) && Objects.equals(propDef, that.propDef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, propDef);
    }
}
