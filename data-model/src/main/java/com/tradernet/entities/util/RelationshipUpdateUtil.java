package com.tradernet.entities.util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Utility for synchronizing collection-based relationships.
 */
public final class RelationshipUpdateUtil {

    private RelationshipUpdateUtil() {
    }

    public static <T> void updateRelationship(Set<T> existing, Set<T> updated, Consumer<T> adder, Consumer<T> remover) {
        Set<T> existingCopy = new HashSet<>(existing);
        Set<T> updatedCopy = new HashSet<>(updated);

        existingCopy.stream()
                .filter(item -> !updatedCopy.contains(item))
                .forEach(remover);

        updatedCopy.stream()
                .filter(item -> !existingCopy.contains(item))
                .forEach(adder);
    }
}
