package com.tradernet.entities.generic;

/**
 * Common interface for entities that expose a primary key identifier.
 */
public interface IdentifiedEntity {
    long getPk();

    void setPk(long id);
}
