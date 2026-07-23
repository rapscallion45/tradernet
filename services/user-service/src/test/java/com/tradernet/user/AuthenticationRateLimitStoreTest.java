package com.tradernet.user;

import com.tradernet.jpa.dao.AuthenticationRateLimitDao;
import com.tradernet.jpa.entities.AuthenticationRateLimitEntity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationRateLimitStoreTest {

    @Test
    void blocksAttemptsOverTheLimitAndAllowsThemAfterTheBlockExpires() {
        final InMemoryRateLimitDao dao = new InMemoryRateLimitDao();
        final AuthenticationRateLimitStore store = new AuthenticationRateLimitStore(dao);
        final Instant now = Instant.parse("2026-07-21T10:00:00Z");

        assertTrue(store.consume("login:key", 2, Duration.ofMinutes(5), Duration.ofMinutes(15), now).isAllowed());
        assertTrue(store.consume("login:key", 2, Duration.ofMinutes(5), Duration.ofMinutes(15), now).isAllowed());

        final RateLimitDecision blocked = store.consume(
            "login:key",
            2,
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            now
        );
        assertFalse(blocked.isAllowed());
        assertTrue(blocked.getRetryAfterSeconds() >= 899);

        assertTrue(store.consume(
            "login:key",
            2,
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            now.plus(Duration.ofMinutes(16))
        ).isAllowed());
    }

    @Test
    void sourceBucketKeysDoNotExposeAddresses() {
        final AuthenticationRateLimitService service = new AuthenticationRateLimitService();

        final String first = service.bucketKey("login", "192.0.2.10");
        final String second = service.bucketKey("login", "192.0.2.11");

        assertFalse(first.contains("192.0.2.10"));
        assertNotEquals(first, second);
    }

    private static final class InMemoryRateLimitDao implements AuthenticationRateLimitDao {
        private final Map<String, AuthenticationRateLimitEntity> buckets = new HashMap<>();

        @Override
        public Optional<AuthenticationRateLimitEntity> findForUpdate(String bucketKey) {
            return Optional.ofNullable(buckets.get(bucketKey));
        }

        @Override
        public void save(AuthenticationRateLimitEntity bucket) {
            buckets.put(bucket.getBucketKey(), bucket);
        }

        @Override
        public int deleteExpired(Instant now) {
            final int previousSize = buckets.size();
            buckets.values().removeIf(bucket -> !bucket.getExpiresAt().isAfter(now));
            return previousSize - buckets.size();
        }
    }
}
