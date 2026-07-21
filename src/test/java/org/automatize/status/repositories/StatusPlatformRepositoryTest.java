package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusPlatform;
import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} coverage for {@link StatusPlatformRepository}'s custom JPQL /
 * derived queries against H2 (PostgreSQL compatibility mode). Focuses on
 * tenant/organization scoping, position ordering, status/visibility filters,
 * search (LIKE), counts and existence checks.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class StatusPlatformRepositoryTest {

    private static final String TENANT_B = "Tenant B";
    private static final String ORG_B = "Org B";
    private static final String THIRD = "Third";
    private static final String THIRD_SLUG = "third";
    private static final String FIRST = "First";
    private static final String SECOND = "Second";
    private static final String CORE_PLATFORM = "Core Platform";

    @Autowired
    private TestEntityManager em;

    @Autowired
    private StatusPlatformRepository repository;

    private Tenant tenant;
    private Organization organization;

    @BeforeEach
    void setUp() {
        tenant = persistTenant("Tenant A");
        organization = persistOrganization("Org A", tenant);
    }

    private Tenant persistTenant(String name) {
        Tenant t = new Tenant();
        t.setName(name);
        t.setCreatedBy("test");
        t.setLastModifiedBy("test");
        return em.persistAndFlush(t);
    }

    private Organization persistOrganization(String name, Tenant tenant) {
        Organization o = new Organization();
        o.setName(name);
        o.setOrganizationType("BUSINESS");
        o.setTenant(tenant);
        o.setCreatedBy("test");
        o.setLastModifiedBy("test");
        return em.persistAndFlush(o);
    }

    private StatusPlatform persistPlatform(String name, String slug, Integer position,
                                           Tenant tenant, Organization org) {
        StatusPlatform p = new StatusPlatform();
        p.setName(name);
        p.setSlug(slug);
        p.setPosition(position);
        p.setTenant(tenant);
        p.setOrganization(org);
        p.setCreatedBy("test");
        p.setLastModifiedBy("test");
        return em.persistAndFlush(p);
    }

    @Test
    void findBySlug_returnsMatchingPlatform() {
        persistPlatform("Core", "core", 0, tenant, organization);

        assertThat(repository.findBySlug("core"))
                .isPresent()
                .get().extracting(StatusPlatform::getName).isEqualTo("Core");
        assertThat(repository.findBySlug("missing")).isEmpty();
    }

    @Test
    void findByTenantIdAndSlug_scopesBySlugWithinTenant() {
        persistPlatform("Core", "core", 0, tenant, organization);

        assertThat(repository.findByTenantIdAndSlug(tenant.getId(), "core")).isPresent();
        assertThat(repository.findByTenantIdAndSlug(tenant.getId(), "nope")).isEmpty();
    }

    @Test
    void findByTenantId_returnsPlatformsOfTenantOnly() {
        persistPlatform("A", "a", 0, tenant, organization);
        Tenant other = persistTenant(TENANT_B);
        Organization otherOrg = persistOrganization(ORG_B, other);
        persistPlatform("B", "b", 0, other, otherOrg);

        assertThat(repository.findByTenantId(tenant.getId()))
                .extracting(StatusPlatform::getName).containsExactly("A");
    }

    @Test
    void findByTenantIdOrderByPosition_ordersAscending() {
        persistPlatform(THIRD, THIRD_SLUG, 3, tenant, organization);
        persistPlatform(FIRST, "first", 1, tenant, organization);
        persistPlatform(SECOND, "second", 2, tenant, organization);

        assertThat(repository.findByTenantIdOrderByPosition(tenant.getId()))
                .extracting(StatusPlatform::getName)
                .containsExactly(FIRST, SECOND, THIRD);
    }

    @Test
    void findByOrganizationId_returnsPlatformsOfOrganizationOnly() {
        persistPlatform("A", "a", 0, tenant, organization);
        Organization otherOrg = persistOrganization(ORG_B, tenant);
        persistPlatform("B", "b", 0, tenant, otherOrg);

        assertThat(repository.findByOrganizationId(organization.getId()))
                .extracting(StatusPlatform::getName).containsExactly("A");
    }

    @Test
    void findByStatus_filtersByStatus() {
        StatusPlatform degraded = persistPlatform("A", "a", 0, tenant, organization);
        degraded.setStatus("DEGRADED");
        em.persistAndFlush(degraded);
        persistPlatform("B", "b", 0, tenant, organization);

        assertThat(repository.findByStatus("DEGRADED"))
                .extracting(StatusPlatform::getName).containsExactly("A");
    }

    @Test
    void findByIsPublic_filtersByVisibility() {
        StatusPlatform priv = persistPlatform("A", "a", 0, tenant, organization);
        priv.setIsPublic(false);
        em.persistAndFlush(priv);
        persistPlatform("B", "b", 0, tenant, organization);

        assertThat(repository.findByIsPublic(false))
                .extracting(StatusPlatform::getName).containsExactly("A");
    }

    @Test
    void findByTenantIdAndIsPublic_combinesTenantAndVisibility() {
        StatusPlatform priv = persistPlatform("A", "a", 0, tenant, organization);
        priv.setIsPublic(false);
        em.persistAndFlush(priv);
        persistPlatform("B", "b", 0, tenant, organization);

        assertThat(repository.findByTenantIdAndIsPublic(tenant.getId(), true))
                .extracting(StatusPlatform::getName).containsExactly("B");
    }

    @Test
    void findByIsPublicTrueOrderByPosition_returnsPublicOrdered() {
        persistPlatform(THIRD, THIRD_SLUG, 3, tenant, organization);
        StatusPlatform priv = persistPlatform("Private", "private", 1, tenant, organization);
        priv.setIsPublic(false);
        em.persistAndFlush(priv);
        persistPlatform(SECOND, "second", 2, tenant, organization);

        assertThat(repository.findByIsPublicTrueOrderByPosition())
                .extracting(StatusPlatform::getName)
                .containsExactly(SECOND, THIRD);
    }

    @Test
    void findAllByOrderByPosition_ordersAscending() {
        persistPlatform(THIRD, THIRD_SLUG, 3, tenant, organization);
        persistPlatform(FIRST, "first", 1, tenant, organization);

        assertThat(repository.findAllByOrderByPosition())
                .extracting(StatusPlatform::getName)
                .containsExactly(FIRST, THIRD);
    }

    @Test
    void searchByTenantId_matchesNameDescriptionOrSlug() {
        StatusPlatform p = persistPlatform(CORE_PLATFORM, "core-plat", 0, tenant, organization);
        p.setDescription("primary infra");
        em.persistAndFlush(p);
        persistPlatform("Web", "web", 0, tenant, organization);

        assertThat(repository.searchByTenantId(tenant.getId(), "Core"))
                .extracting(StatusPlatform::getName).containsExactly(CORE_PLATFORM);
        assertThat(repository.searchByTenantId(tenant.getId(), "infra"))
                .extracting(StatusPlatform::getName).containsExactly(CORE_PLATFORM);
    }

    @Test
    void search_matchesGloballyBySlug() {
        persistPlatform(CORE_PLATFORM, "core-plat", 0, tenant, organization);
        persistPlatform("Web", "web", 0, tenant, organization);

        assertThat(repository.search("web"))
                .extracting(StatusPlatform::getName).containsExactly("Web");
    }

    @Test
    void countByTenantId_countsScopedPlatforms() {
        persistPlatform("A", "a", 0, tenant, organization);
        persistPlatform("B", "b", 0, tenant, organization);
        Tenant other = persistTenant(TENANT_B);
        Organization otherOrg = persistOrganization(ORG_B, other);
        persistPlatform("C", "c", 0, other, otherOrg);

        assertThat(repository.countByTenantId(tenant.getId())).isEqualTo(2L);
    }

    @Test
    void existsBySlug_reflectsPresence() {
        persistPlatform("A", "a", 0, tenant, organization);

        assertThat(repository.existsBySlug("a")).isTrue();
        assertThat(repository.existsBySlug("nope")).isFalse();
    }

    @Test
    void existsByTenantIdAndSlug_scopesToTenant() {
        persistPlatform("A", "a", 0, tenant, organization);
        Tenant other = persistTenant(TENANT_B);

        assertThat(repository.existsByTenantIdAndSlug(tenant.getId(), "a")).isTrue();
        assertThat(repository.existsByTenantIdAndSlug(other.getId(), "a")).isFalse();
    }
}
