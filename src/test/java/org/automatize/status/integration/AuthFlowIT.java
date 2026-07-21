package org.automatize.status.integration;

import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * End-to-end authentication flow through the real JWT filter chain.
 *
 * <p>Verifies login token issuance, that a Bearer token authenticates a request
 * to a protected endpoint, that the same endpoint is rejected with 401 when
 * unauthenticated, and that bad credentials are rejected.</p>
 */
class AuthFlowIT extends AbstractIntegrationIT {

    private static final String USERNAME = "admin.user";
    private static final String PASSWORD = "S3cret!pass";

    @BeforeEach
    void seed() {
        Tenant tenant = persistTenant("Auth Tenant");
        Organization org = persistOrganization("Auth Org", tenant);
        persistUser(USERNAME, PASSWORD, "ADMIN", org);
    }

    /** (a) Correct credentials return 200 with an {@code accessToken} (and Bearer type) in the JSON. */
    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(USERNAME, PASSWORD))
                        .with(uniqueIp()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value(USERNAME));
    }

    /** (b) A protected endpoint is reachable (NOT 401) when a valid Bearer token is supplied. */
    @Test
    void protectedEndpoint_withBearerToken_isNotUnauthorized() throws Exception {
        String token = obtainAccessToken(USERNAME, PASSWORD);

        mockMvc.perform(get("/api/components")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    /** (c) The same protected endpoint returns 401 when no token is supplied. */
    @Test
    void protectedEndpoint_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/components"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * (d) Wrong password is rejected. The {@code BadCredentialsException} thrown
     * by the {@code AuthenticationManager} propagates to Spring Security's
     * {@code ExceptionTranslationFilter}, which invokes the JWT entry point and
     * returns 401 for {@code /api/**} paths.
     */
    @Test
    void login_withWrongPassword_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(USERNAME, "wrong-password"))
                        .with(uniqueIp()))
                .andExpect(status().isUnauthorized());
    }
}
