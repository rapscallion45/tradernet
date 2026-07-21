package com.tradernet.jpa.identity;

import com.tradernet.jpa.entities.UserEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsernameNormalizerTest {

    @Test
    void normalizesWidthWhitespaceAndCaseConsistently() {
        assertEquals("alice", UsernameNormalizer.normalize("  \uFF21LiCe  "));
    }

    @Test
    void userEntityMaintainsItsCanonicalUsername() {
        final UserEntity user = new UserEntity("  Alice  ");

        assertEquals("alice", user.getNormalizedUsername());

        user.setUsername("BOB");
        assertEquals("bob", user.getNormalizedUsername());
    }
}
