package org.automatize.status.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LoginRateLimiter}.
 *
 * <p>MAX_ATTEMPTS is 10 within a 60s window: the 11th attempt from the same IP is blocked.</p>
 */
class LoginRateLimiterTest {

    private LoginRateLimiter rateLimiter;

    /**
     * Creates a fresh {@link LoginRateLimiter} before each test so per-IP
     * attempt windows do not carry over between tests.
     */
    @BeforeEach
    void setUp() {
        rateLimiter = new LoginRateLimiter();
    }

    /**
     * Verifies that the first ten attempts from a single IP are all allowed
     * (within the budget).
     */
    @Test
    void isAllowed_underLimit_returnsTrue() {
        // Act & Assert: first 10 attempts are allowed
        for (int i = 1; i <= 10; i++) {
            assertThat(rateLimiter.isAllowed("10.0.0.1"))
                    .as("attempt %d should be allowed", i)
                    .isTrue();
        }
    }

    /**
     * Verifies that once the ten-attempt budget is exhausted, the eleventh
     * attempt from the same IP is blocked.
     */
    @Test
    void isAllowed_overLimit_returnsFalse() {
        // Arrange: exhaust the budget of 10 attempts
        for (int i = 1; i <= 10; i++) {
            rateLimiter.isAllowed("10.0.0.2");
        }

        // Act: the 11th attempt
        boolean eleventh = rateLimiter.isAllowed("10.0.0.2");

        // Assert
        assertThat(eleventh).isFalse();
    }

    /**
     * Verifies that attempt budgets are tracked per IP: blocking one IP does not
     * affect a different, fresh IP.
     */
    @Test
    void isAllowed_differentIps_trackedIndependently() {
        // Arrange: block the first IP
        for (int i = 1; i <= 11; i++) {
            rateLimiter.isAllowed("10.0.0.3");
        }

        // Act: a fresh IP starts with a clean window
        boolean freshIpAllowed = rateLimiter.isAllowed("10.0.0.99");

        // Assert
        assertThat(rateLimiter.isAllowed("10.0.0.3")).isFalse();
        assertThat(freshIpAllowed).isTrue();
    }

    /**
     * Verifies that the scheduled sweep leaves buckets whose window is still running,
     * so an active client does not silently lose its recorded attempts.
     */
    @Test
    void evictExpired_keepsBucketsInsideCurrentWindow() {
        // Arrange: three IPs recorded just now
        rateLimiter.isAllowed("10.0.1.1");
        rateLimiter.isAllowed("10.0.1.2");
        rateLimiter.isAllowed("10.0.1.3");

        // Act
        rateLimiter.evictExpired();

        // Assert
        assertThat(buckets()).hasSize(3);
    }

    /**
     * Verifies that the scheduled sweep removes buckets whose window has elapsed — the
     * behaviour that keeps the map from growing for every IP that ever hit the endpoint.
     */
    @Test
    void evictExpired_removesBucketsWithElapsedWindow() {
        // Arrange: one stale bucket (window started two minutes ago) and one fresh
        putBucket("10.0.2.1", 3, System.currentTimeMillis() - 120_000L);
        rateLimiter.isAllowed("10.0.2.2");

        // Act
        rateLimiter.evictExpired();

        // Assert
        assertThat(buckets()).containsOnlyKeys("10.0.2.2");
    }

    /**
     * Verifies the hard cap: once the tracked-IP limit is reached within a single window,
     * a further unseen IP sheds the map instead of growing it without bound.
     */
    @Test
    void isAllowed_atTrackedIpCap_shedsBucketsInsteadOfGrowing() {
        // Arrange: fill the map to its 10_000 IP capacity within one window
        for (int i = 0; i < 10_000; i++) {
            rateLimiter.isAllowed("10.1." + (i / 256) + "." + (i % 256));
        }
        assertThat(buckets()).hasSize(10_000);

        // Act: an unseen IP arrives with nothing expired to reclaim
        boolean allowed = rateLimiter.isAllowed("172.16.0.1");

        // Assert: the map was shed and now holds only the newcomer
        assertThat(allowed).isTrue();
        assertThat(buckets()).containsOnlyKeys("172.16.0.1");
    }

    /**
     * Verifies that an already-tracked IP keeps its running count when the map is at
     * capacity: only an unseen IP can grow the map, so only that path sheds entries.
     */
    @Test
    void isAllowed_atTrackedIpCap_keepsCountOfKnownIp() {
        // Arrange: fill to capacity, with the last IP already over its budget
        String knownIp = "10.2.0.1";
        for (int i = 0; i < 9_999; i++) {
            rateLimiter.isAllowed("10.3." + (i / 256) + "." + (i % 256));
        }
        for (int i = 1; i <= 11; i++) {
            rateLimiter.isAllowed(knownIp);
        }
        assertThat(buckets()).hasSize(10_000);

        // Act & Assert: the known IP stays blocked and the map is untouched
        assertThat(rateLimiter.isAllowed(knownIp)).isFalse();
        assertThat(buckets()).hasSize(10_000);
    }

    // -------------------------------------------------------------------------
    // Test helpers
    // -------------------------------------------------------------------------

    /** Returns the limiter's internal per-IP bucket map. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buckets() {
        return (Map<String, Object>) ReflectionTestUtils.getField(rateLimiter, "buckets");
    }

    /**
     * Inserts a bucket directly into the limiter with an explicit window start, so that an
     * elapsed window can be simulated without waiting a minute.
     *
     * @param clientIp    the IP to record
     * @param count       the attempt count to store
     * @param windowStart the epoch milliseconds the window started at
     */
    private void putBucket(String clientIp, int count, long windowStart) {
        try {
            Class<?> bucketClass = Class.forName("org.automatize.status.security.LoginRateLimiter$Bucket");
            Constructor<?> constructor = bucketClass.getDeclaredConstructor(AtomicInteger.class, long.class);
            constructor.setAccessible(true);
            buckets().put(clientIp, constructor.newInstance(new AtomicInteger(count), windowStart));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to seed a rate limiter bucket", e);
        }
    }
}
