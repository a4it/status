package org.automatize.status.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * <p>
 * Utility class for generating secure API keys.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Generate cryptographically secure random API keys</li>
 *   <li>Produce URL-safe Base64 encoded key strings</li>
 *   <li>Provide a consistent key length for application use</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
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
