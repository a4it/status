package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusPlatform;
import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} coverage for {@link StatusAppRepository}'s custom JPQL /
 * derived queries against H2 (PostgreSQL compatibility mode). Focuses on
 * tenant/organization/platform scoping, status/visibility filters, search
 * (LIKE), counts, existence checks, API key lookup and health-check filtering.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class StatusAppRepositoryTest extends AbstractRepositoryTest {

    private static final String SLUG_PAYMENTS = "payments";
    private static final String NAME_PAYMENTS = "Payments";
    private static final String TENANT_B = "Tenant B";
    private static final String ORG_B = "Org B";
    private static final String STATUS_DEGRADED = "DEGRADED";
    private static final String NAME_PAYMENT_API = "Payment API";
    private static final String SLUG_PAY_API = "pay-api";

    @Autowired
    private StatusAppRepository repository;

    private Tenant tenant;
    private Organization organization;

    @BeforeEach
    void setUp() {
        tenant = persistTenant("Tenant A");
        organization = persistOrganization("Org A", tenant);
    }

    private StatusPlatform persistPlatform(String name, String slug, Tenant tenant, Organization org) {
        StatusPlatform p = new StatusPlatform();
        p.setName(name);
        p.setSlug(slug);
        p.setTenant(tenant);
        p.setOrganization(org);
        p.setCreatedBy("test");
        p.setLastModifiedBy("test");
        return em.persistAndFlush(p);
    }

    private StatusApp persistApp(String name, String slug, Tenant tenant, Organization org) {
        StatusApp a = new StatusApp();
        a.setName(name);
        a.setSlug(slug);
        a.setTenant(tenant);
        a.setOrganization(org);
        a.setCreatedBy("test");
        a.setLastModifiedBy("test");
        return em.persistAndFlush(a);
    }

    @Test
    void findBySlug_returnsMatchingApp() {
        persistApp(NAME_PAYMENTS, SLUG_PAYMENTS, tenant, organization);

        assertThat(repository.findBySlug(SLUG_PAYMENTS))
                .isPresent()
                .get().extracting(StatusApp::getName).isEqualTo(NAME_PAYMENTS);
        assertThat(repository.findBySlug("missing")).isEmpty();
    }

    @Test
    void findByTenantIdAndSlug_scopesBySlugWithinTenant() {
        persistApp(NAME_PAYMENTS, SLUG_PAYMENTS, tenant, organization);

        assertThat(repository.findByTenantIdAndSlug(tenant.getId(), SLUG_PAYMENTS)).isPresent();
        assertThat(repository.findByTenantIdAndSlug(tenant.getId(), "other")).isEmpty();
    }

    @Test
    void findByTenantId_returnsAppsOfTenantOnly() {
        persistApp("A", "a", tenant, organization);
        Tenant other = persistTenant(TENANT_B);
        Organization otherOrg = persistOrganization(ORG_B, other);
        persistApp("B", "b", other, otherOrg);

        List<StatusApp> result = repository.findByTenantId(tenant.getId());

        assertThat(result).extracting(StatusApp::getName).containsExactly("A");
    }

    @Test
    void findByOrganizationId_returnsAppsOfOrganizationOnly() {
        persistApp("A", "a", tenant, organization);
        Organization otherOrg = persistOrganization(ORG_B, tenant);
        persistApp("B", "b", tenant, otherOrg);

        assertThat(repository.findByOrganizationId(organization.getId()))
                .extracting(StatusApp::getName).containsExactly("A");
    }

    @Test
    void findByPlatformId_returnsAppsOfPlatform() {
        StatusPlatform platform = persistPlatform("Platform", "platform", tenant, organization);
        StatusApp app = new StatusApp();
        app.setName("A");
        app.setSlug("a");
        app.setTenant(tenant);
        app.setOrganization(organization);
        app.setPlatform(platform);
        app.setCreatedBy("test");
        app.setLastModifiedBy("test");
        em.persistAndFlush(app);
        persistApp("NoPlatform", "np", tenant, organization);

        assertThat(repository.findByPlatformId(platform.getId()))
                .extracting(StatusApp::getName).containsExactly("A");
    }

    @Test
    void findByStatus_filtersByStatus() {
        StatusApp degraded = persistApp("A", "a", tenant, organization);
        degraded.setStatus(STATUS_DEGRADED);
        em.persistAndFlush(degraded);
        persistApp("B", "b", tenant, organization); // default OPERATIONAL

        assertThat(repository.findByStatus(STATUS_DEGRADED))
                .extracting(StatusApp::getName).containsExactly("A");
    }

    @Test
    void findByIsPublic_filtersByVisibility() {
        StatusApp priv = persistApp("A", "a", tenant, organization);
        priv.setIsPublic(false);
        em.persistAndFlush(priv);
        persistApp("B", "b", tenant, organization); // default public

        assertThat(repository.findByIsPublic(false))
                .extracting(StatusApp::getName).containsExactly("A");
        assertThat(repository.findByIsPublic(true))
                .extracting(StatusApp::getName).containsExactly("B");
    }

    @Test
    void findByTenantIdAndIsPublic_combinesTenantAndVisibility() {
        StatusApp priv = persistApp("A", "a", tenant, organization);
        priv.setIsPublic(false);
        em.persistAndFlush(priv);
        persistApp("B", "b", tenant, organization);

        assertThat(repository.findByTenantIdAndIsPublic(tenant.getId(), true))
                .extracting(StatusApp::getName).containsExactly("B");
    }

    @Test
    void findByOrganizationIdAndStatus_combinesOrgAndStatus() {
        StatusApp degraded = persistApp("A", "a", tenant, organization);
        degraded.setStatus(STATUS_DEGRADED);
        em.persistAndFlush(degraded);
        persistApp("B", "b", tenant, organization);

        assertThat(repository.findByOrganizationIdAndStatus(organization.getId(), STATUS_DEGRADED))
                .extracting(StatusApp::getName).containsExactly("A");
    }

    @Test
    void searchByTenantId_matchesNameDescriptionOrSlug() {
        StatusApp app = persistApp(NAME_PAYMENT_API, SLUG_PAY_API, tenant, organization);
        app.setDescription("handles billing");
        em.persistAndFlush(app);
        persistApp("Web", "web", tenant, organization);

        assertThat(repository.searchByTenantId(tenant.getId(), "API"))
                .extracting(StatusApp::getName).containsExactly(NAME_PAYMENT_API);
        assertThat(repository.searchByTenantId(tenant.getId(), "billing"))
                .extracting(StatusApp::getName).containsExactly(NAME_PAYMENT_API);
        assertThat(repository.searchByTenantId(tenant.getId(), SLUG_PAY_API))
                .extracting(StatusApp::getName).containsExactly(NAME_PAYMENT_API);
    }

    @Test
    void searchByOrganizationId_matchesNameOrDescription() {
        persistApp(NAME_PAYMENT_API, SLUG_PAY_API, tenant, organization);
        persistApp("Web", "web", tenant, organization);

        assertThat(repository.searchByOrganizationId(organization.getId(), "Payment"))
                .extracting(StatusApp::getName).containsExactly(NAME_PAYMENT_API);
    }

    @Test
    void search_matchesGloballyBySlug() {
        persistApp(NAME_PAYMENT_API, SLUG_PAY_API, tenant, organization);
        persistApp("Web", "web", tenant, organization);

        assertThat(repository.search("web"))
                .extracting(StatusApp::getName).containsExactly("Web");
    }

    @Test
    void countByTenantId_countsScopedApps() {
        persistApp("A", "a", tenant, organization);
        persistApp("B", "b", tenant, organization);
        Tenant other = persistTenant(TENANT_B);
        Organization otherOrg = persistOrganization(ORG_B, other);
        persistApp("C", "c", other, otherOrg);

        assertThat(repository.countByTenantId(tenant.getId())).isEqualTo(2L);
    }

    @Test
    void countByOrganizationId_countsScopedApps() {
        persistApp("A", "a", tenant, organization);
        persistApp("B", "b", tenant, organization);

        assertThat(repository.countByOrganizationId(organization.getId())).isEqualTo(2L);
    }

    @Test
    void existsBySlug_reflectsPresence() {
        persistApp("A", "a", tenant, organization);

        assertThat(repository.existsBySlug("a")).isTrue();
        assertThat(repository.existsBySlug("nope")).isFalse();
    }

    @Test
    void existsByTenantIdAndSlug_scopesToTenant() {
        persistApp("A", "a", tenant, organization);
        Tenant other = persistTenant(TENANT_B);

        assertThat(repository.existsByTenantIdAndSlug(tenant.getId(), "a")).isTrue();
        assertThat(repository.existsByTenantIdAndSlug(other.getId(), "a")).isFalse();
    }

    @Test
    void findByApiKey_returnsMatchingApp() {
        StatusApp app = persistApp("A", "a", tenant, organization);
        app.setApiKey("secret-key-123");
        em.persistAndFlush(app);

        assertThat(repository.findByApiKey("secret-key-123"))
                .isPresent()
                .get().extracting(StatusApp::getName).isEqualTo("A");
        assertThat(repository.findByApiKey("wrong")).isEmpty();
    }

    @Test
    void findCheckEnabledApps_returnsOnlyEligibleApps() {
        StatusApp enabled = persistApp("Enabled", "enabled", tenant, organization);
        enabled.setCheckEnabled(true);
        enabled.setCheckType("HTTP");
        enabled.setCheckUrl("http://example.com/health");
        em.persistAndFlush(enabled);

        // Disabled/default app should be excluded (checkEnabled=false, checkType=NONE).
        persistApp("Disabled", "disabled", tenant, organization);

        // Enabled but missing URL should be excluded.
        StatusApp noUrl = persistApp("NoUrl", "no-url", tenant, organization);
        noUrl.setCheckEnabled(true);
        noUrl.setCheckType("HTTP");
        em.persistAndFlush(noUrl);

        assertThat(repository.findCheckEnabledApps())
                .extracting(StatusApp::getName).containsExactly("Enabled");
    }
}
