package org.automatize.status.controllers.api;

import org.automatize.status.api.response.ContextResponse;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.security.UserPrincipal;
import org.automatize.status.services.TenantContextService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link TenantContextController}. Security filters and
 * method security are disabled ({@code addFilters = false}); focus is request
 * mapping, JSON contract, {@code @ResponseStatus} exception mapping (404), and
 * delegation to the (mocked) service layer.
 * <p>
 * Endpoints that resolve {@code @AuthenticationPrincipal UserPrincipal} require
 * an {@code Authentication} in the {@link SecurityContextHolder}; it is seeded
 * per test since {@code addFilters = false} skips the JWT filter chain.
 */
@WebMvcTest(controllers = TenantContextController.class)
class TenantContextControllerTest extends AbstractApiControllerTest {

    /**
     * The WebMvc slice does not register Spring Security's
     * {@code @AuthenticationPrincipal} resolver, so without this the
     * {@link UserPrincipal} parameter falls through to model binding and 400s.
     */
    @TestConfiguration
    static class SecurityArgumentResolverConfig implements WebMvcConfigurer {
        /**
         * Registers the {@link AuthenticationPrincipalArgumentResolver} so that
         * {@code @AuthenticationPrincipal UserPrincipal} parameters resolve
         * correctly within the WebMvc slice.
         *
         * @param resolvers the mutable list of argument resolvers to contribute to
         */
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @MockitoBean
    private TenantContextService tenantContextService;

    private final UUID principalId = UUID.randomUUID();

    /**
     * Seeds the {@link SecurityContextHolder} with an authenticated SUPERADMIN
     * {@link UserPrincipal} before each test, since {@code addFilters = false}
     * skips the JWT filter chain that would normally populate it.
     */
    @BeforeEach
    void setUpPrincipal() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
        UserPrincipal principal = new UserPrincipal(
                principalId, "superadmin", "superadmin@example.com", "pw",
                "SUPERADMIN", null, true, authorities);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    /**
     * Clears the {@link SecurityContextHolder} after each test to avoid leaking
     * authentication state between tests.
     */
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Builds a minimal active {@link Tenant} fixture for use in stubbed service
     * responses.
     *
     * @param id the identifier to assign to the tenant
     * @return a populated sample {@link Tenant}
     */
    private Tenant sampleTenant(UUID id) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setName("Globex");
        t.setIsActive(true);
        return t;
    }

    /**
     * Builds a minimal active {@link Organization} fixture for use in stubbed
     * service responses.
     *
     * @param id the identifier to assign to the organization
     * @return a populated sample {@link Organization}
     */
    private Organization sampleOrg(UUID id) {
        Organization o = new Organization();
        o.setId(id);
        o.setName("Acme");
        o.setStatus("ACTIVE");
        return o;
    }

    /**
     * Builds a sample {@link ContextResponse} representing a fully selected
     * superadmin context, used to stub context switch/current-context calls.
     *
     * @return a populated sample {@link ContextResponse}
     */
    private ContextResponse sampleContext() {
        ContextResponse r = new ContextResponse();
        r.setAccessToken("ctx-token");
        r.setTenantName("Globex");
        r.setOrganizationName("Acme");
        r.setSuperadmin(true);
        r.setHasSelectedContext(true);
        return r;
    }

    /**
     * Verifies that GET {@code /api/context/tenants} returns 200 with a JSON
     * array of the active tenants supplied by the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getTenants_returns200List() throws Exception {
        when(tenantContextService.getActiveTenants())
                .thenReturn(List.of(sampleTenant(UUID.randomUUID())));

        mockMvc.perform(get("/api/context/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Globex"));
    }

    /**
     * Verifies that GET {@code /api/context/tenants/{tenantId}/organizations}
     * returns 200 with a JSON array of the organizations for the given tenant.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getOrganizations_returns200List() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(tenantContextService.getOrganizationsForTenant(tenantId))
                .thenReturn(List.of(sampleOrg(UUID.randomUUID())));

        mockMvc.perform(get("/api/context/tenants/{tenantId}/organizations", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Acme"));
    }

    /**
     * Verifies that POST {@code /api/context/switch} with a valid tenant and
     * organization returns 200 and the resulting context payload (access token
     * and selected-context flag).
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void switchContext_valid_returns200() throws Exception {
        when(tenantContextService.switchContext(any(), any(), any())).thenReturn(sampleContext());

        String body = "{\"tenantId\":\"" + UUID.randomUUID() + "\",\"organizationId\":\"" + UUID.randomUUID() + "\"}";
        mockMvc.perform(post("/api/context/switch").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("ctx-token"))
                .andExpect(jsonPath("$.hasSelectedContext").value(true));
    }

    /**
     * Verifies that POST {@code /api/context/switch} returns 404 when the
     * service raises {@link ResourceNotFoundException} for an unknown tenant.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void switchContext_tenantNotFound_returns404() throws Exception {
        when(tenantContextService.switchContext(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Tenant not found"));

        String body = "{\"tenantId\":\"" + UUID.randomUUID() + "\",\"organizationId\":\"" + UUID.randomUUID() + "\"}";
        mockMvc.perform(post("/api/context/switch").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies that GET {@code /api/context/current} returns 200 with the
     * current context details resolved for the authenticated principal.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getCurrentContext_returns200() throws Exception {
        when(tenantContextService.getCurrentContext(any())).thenReturn(sampleContext());

        mockMvc.perform(get("/api/context/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantName").value("Globex"))
                .andExpect(jsonPath("$.superadmin").value(true));
    }
}
