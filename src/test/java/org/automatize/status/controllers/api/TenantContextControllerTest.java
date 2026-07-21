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
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @MockitoBean
    private TenantContextService tenantContextService;

    private final UUID principalId = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
        UserPrincipal principal = new UserPrincipal(
                principalId, "superadmin", "superadmin@example.com", "pw",
                "SUPERADMIN", null, true, authorities);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Tenant sampleTenant(UUID id) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setName("Globex");
        t.setIsActive(true);
        return t;
    }

    private Organization sampleOrg(UUID id) {
        Organization o = new Organization();
        o.setId(id);
        o.setName("Acme");
        o.setStatus("ACTIVE");
        return o;
    }

    private ContextResponse sampleContext() {
        ContextResponse r = new ContextResponse();
        r.setAccessToken("ctx-token");
        r.setTenantName("Globex");
        r.setOrganizationName("Acme");
        r.setSuperadmin(true);
        r.setHasSelectedContext(true);
        return r;
    }

    @Test
    void getTenants_returns200List() throws Exception {
        when(tenantContextService.getActiveTenants())
                .thenReturn(List.of(sampleTenant(UUID.randomUUID())));

        mockMvc.perform(get("/api/context/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Globex"));
    }

    @Test
    void getOrganizations_returns200List() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(tenantContextService.getOrganizationsForTenant(tenantId))
                .thenReturn(List.of(sampleOrg(UUID.randomUUID())));

        mockMvc.perform(get("/api/context/tenants/{tenantId}/organizations", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Acme"));
    }

    @Test
    void switchContext_valid_returns200() throws Exception {
        when(tenantContextService.switchContext(any(), any(), any())).thenReturn(sampleContext());

        String body = "{\"tenantId\":\"" + UUID.randomUUID() + "\",\"organizationId\":\"" + UUID.randomUUID() + "\"}";
        mockMvc.perform(post("/api/context/switch").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("ctx-token"))
                .andExpect(jsonPath("$.hasSelectedContext").value(true));
    }

    @Test
    void switchContext_tenantNotFound_returns404() throws Exception {
        when(tenantContextService.switchContext(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Tenant not found"));

        String body = "{\"tenantId\":\"" + UUID.randomUUID() + "\",\"organizationId\":\"" + UUID.randomUUID() + "\"}";
        mockMvc.perform(post("/api/context/switch").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCurrentContext_returns200() throws Exception {
        when(tenantContextService.getCurrentContext(any())).thenReturn(sampleContext());

        mockMvc.perform(get("/api/context/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantName").value("Globex"))
                .andExpect(jsonPath("$.superadmin").value(true));
    }
}
