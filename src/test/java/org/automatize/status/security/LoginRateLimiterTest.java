package org.automatize.status.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LoginRateLimiter}.
 *
 * <p>MAX_ATTEMPTS is 10 within a 60s window: the 11th attempt from the same IP is blocked.</p>
 */
class LoginRateLimiterTest {

    private LoginRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new LoginRateLimiter();
    }

    @Test
    void isAllowed_underLimit_returnsTrue() {
        // Act & Assert: first 10 attempts are allowed
        for (int i = 1; i <= 10; i++) {
            assertThat(rateLimiter.isAllowed("10.0.0.1"))
                    .as("attempt %d should be allowed", i)
                    .isTrue();
        }
    }

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
}
