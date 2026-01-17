package org.automatize.status.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for generating secure API keys.
 */
public final class ApiKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int API_KEY_BYTES = 32;

    private ApiKeyGenerator() {
    }

    /**
     * Generates a secure random API key.
     *
     * @return a 43-character Base64 URL-safe API key
     */
    public static String generateApiKey() {
        byte[] bytes = new byte[API_KEY_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
