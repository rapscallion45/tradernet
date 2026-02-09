package com.tradernet.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Definition for a user property name.
 */
@Embeddable
public class UserPropertyDefinition {
    @Column(name = "propertyName")
    private String name;

    public UserPropertyDefinition() {
    }

    public UserPropertyDefinition(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserPropertyDefinition)) {
            return false;
        }
        UserPropertyDefinition that = (UserPropertyDefinition) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
