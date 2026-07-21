package org.automatize.status.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that endpoints under {@code /api/public/**} are reachable WITHOUT
 * authentication (they are {@code permitAll} in {@link org.automatize.status.config.SecurityConfig}).
 */
class PublicStatusIT extends AbstractIntegrationIT {

    /**
     * The public "list apps" endpoint responds without a token and is not 401.
     * With no seeded public apps it returns 200 with an empty JSON array — the
     * empty-result contract of {@code PublicStatusController#getAllPublicApps}.
     */
    @Test
    void publicApps_withoutAuth_returnsOkEmptyList() throws Exception {
        mockMvc.perform(get("/api/public/status/apps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * The public status summary endpoint is likewise reachable unauthenticated
     * (asserts it is specifically NOT 401 Unauthorized).
     */
    @Test
    void publicSummary_withoutAuth_isNotUnauthorized() throws Exception {
        int statusCode = mockMvc.perform(get("/api/public/status/summary"))
                .andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Public status endpoint must be reachable without authentication");
    }
}
