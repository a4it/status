package org.automatize.status.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApiKeyGenerator}.
 *
 * <p>32 random bytes Base64 URL-encoded without padding yields a 43-character key.</p>
 */
class ApiKeyGeneratorTest {

    @Test
    void generateApiKey_always_returnsNonNullNonBlankKey() {
        // Act
        String key = ApiKeyGenerator.generateApiKey();

        // Assert
        assertThat(key).isNotNull().isNotBlank();
    }

    @Test
    void generateApiKey_always_hasExpectedLength() {
        // Act
        String key = ApiKeyGenerator.generateApiKey();

        // Assert: 32 bytes -> ceil(32/3)*4 = 44, minus 1 padding char = 43
        assertThat(key).hasSize(43);
    }

    @Test
    void generateApiKey_always_isUrlSafeBase64WithoutPadding() {
        // Act
        String key = ApiKeyGenerator.generateApiKey();

        // Assert: only URL-safe Base64 alphabet, no '+', '/', or '=' padding
        assertThat(key).matches("^[A-Za-z0-9_-]+$");
        assertThat(key).doesNotContain("=");
    }

    @Test
    void generateApiKey_manyCalls_producesUniqueKeys() {
        // Arrange
        int iterations = 10_000;
        Set<String> keys = new HashSet<>();

        // Act
        for (int i = 0; i < iterations; i++) {
            keys.add(ApiKeyGenerator.generateApiKey());
        }

        // Assert: no collisions across many calls (entropy check)
        assertThat(keys).hasSize(iterations);
    }
}
