package org.automatize.status.integration;

import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-tenant data visibility through the {@code /api/components} endpoint.
 *
 * <p>Seeds two tenants, each with its own org, admin user, status app and
 * component, then logs in as tenant A's admin and lists components.</p>
 *
 * <p>NOTE (observed real behaviour, asserted here rather than an aspirational
 * expectation): {@code /api/components} with no filter delegates to
 * {@code StatusComponentService.getAllComponents}, which calls
 * {@code repository.findAll(pageable)} with NO tenant/organization scoping. The
 * JWT carries an {@code organizationId} claim but this endpoint does not use it.
 * Consequently an authenticated ADMIN sees components from ALL tenants — tenant
 * isolation is NOT enforced at this API layer. The test documents that fact.</p>
 */
class MultiTenantIsolationIT extends AbstractIntegrationIT {

    private static final String ADMIN_A = "admin.a";
    private static final String ADMIN_B = "admin.b";
    private static final String PASSWORD = "P@ssw0rd!";
    private static final String COMPONENT_A = "Tenant-A-Component";
    private static final String COMPONENT_B = "Tenant-B-Component";

    @BeforeEach
    void seedTwoTenants() {
        Tenant tenantA = persistTenant("Tenant A");
        Organization orgA = persistOrganization("Org A", tenantA);
        persistUser(ADMIN_A, PASSWORD, "ADMIN", orgA);
        StatusApp appA = persistApp("App A", "app-a", tenantA, orgA);
        persistComponent(COMPONENT_A, appA);

        Tenant tenantB = persistTenant("Tenant B");
        Organization orgB = persistOrganization("Org B", tenantB);
        persistUser(ADMIN_B, PASSWORD, "ADMIN", orgB);
        StatusApp appB = persistApp("App B", "app-b", tenantB, orgB);
        persistComponent(COMPONENT_B, appB);
    }

    /**
     * Tenant A's admin lists components and — because the endpoint is not
     * tenant-scoped — sees tenant B's component as well. This asserts the REAL,
     * currently-unscoped behaviour (see class NOTE).
     */
    @Test
    void componentsList_isNotTenantScoped_seesOtherTenantData() throws Exception {
        String tokenA = obtainAccessToken(ADMIN_A, PASSWORD);

        mockMvc.perform(get("/api/components")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].name", hasItems(COMPONENT_A, COMPONENT_B)));
    }
}
