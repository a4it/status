package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.StatusMaintenance;
import org.automatize.status.models.StatusMaintenanceComponent;
import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} coverage for {@link StatusMaintenanceComponentRepository}'s
 * custom JPQL / derived queries against H2 (PostgreSQL compatibility mode).
 * Focuses on maintenance/component association lookups, tenant/organization/app
 * scoping (deep path traversal), counts, active-maintenance queries, existence
 * checks and derived deletes.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class StatusMaintenanceComponentRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private StatusMaintenanceComponentRepository repository;

    private Tenant tenant;
    private Organization organization;
    private StatusApp app;
    private StatusComponent component;
    private StatusMaintenance maintenance;

    private final ZonedDateTime now = ZonedDateTime.now();

    @BeforeEach
    void setUp() {
        tenant = persistTenant("Tenant A");
        organization = persistOrganization("Org A", tenant);
        app = persistApp("App A", "app-a", tenant, organization);
        component = persistComponent("API", app);
        maintenance = persistMaintenance("Patch", "SCHEDULED", app);
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

    private StatusComponent persistComponent(String name, StatusApp app) {
        StatusComponent c = new StatusComponent();
        c.setName(name);
        c.setApp(app);
        c.setCreatedBy("test");
        c.setLastModifiedBy("test");
        return em.persistAndFlush(c);
    }

    private StatusMaintenance persistMaintenance(String title, String status, StatusApp app) {
        StatusMaintenance m = new StatusMaintenance();
        m.setTitle(title);
        m.setStatus(status);
        m.setStartsAt(now);
        m.setEndsAt(now.plusHours(1));
        m.setApp(app);
        m.setCreatedBy("test");
        m.setLastModifiedBy("test");
        return em.persistAndFlush(m);
    }

    private StatusMaintenanceComponent persistLink(StatusMaintenance maintenance, StatusComponent component) {
        StatusMaintenanceComponent mc = new StatusMaintenanceComponent();
        mc.setMaintenance(maintenance);
        mc.setComponent(component);
        return em.persistAndFlush(mc);
    }

    @Test
    void findByMaintenanceId_returnsLinksForMaintenance() {
        persistLink(maintenance, component);
        StatusMaintenance other = persistMaintenance("Other", "SCHEDULED", app);
        StatusComponent otherComp = persistComponent("Web", app);
        persistLink(other, otherComp);

        assertThat(repository.findByMaintenanceId(maintenance.getId()))
                .extracting(mc -> mc.getComponent().getName()).containsExactly("API");
    }

    @Test
    void findByComponentId_returnsLinksForComponent() {
        persistLink(maintenance, component);
        StatusMaintenance other = persistMaintenance("Other", "SCHEDULED", app);
        persistLink(other, component);

        assertThat(repository.findByComponentId(component.getId())).hasSize(2);
    }

    @Test
    void findByMaintenanceIdAndComponentId_returnsSpecificLink() {
        persistLink(maintenance, component);
        StatusComponent other = persistComponent("Web", app);

        assertThat(repository.findByMaintenanceIdAndComponentId(maintenance.getId(), component.getId())).isPresent();
        assertThat(repository.findByMaintenanceIdAndComponentId(maintenance.getId(), other.getId())).isEmpty();
    }

    @Test
    void findByTenantId_scopesByMaintenanceAppTenant() {
        persistLink(maintenance, component);
        Tenant other = persistTenant("Tenant B");
        Organization otherOrg = persistOrganization("Org B", other);
        StatusApp otherApp = persistApp("App B", "app-b", other, otherOrg);
        StatusComponent otherComp = persistComponent("OtherComp", otherApp);
        StatusMaintenance otherM = persistMaintenance("OtherM", "SCHEDULED", otherApp);
        persistLink(otherM, otherComp);

        assertThat(repository.findByTenantId(tenant.getId()))
                .extracting(mc -> mc.getComponent().getName()).containsExactly("API");
    }

    @Test
    void findByOrganizationId_scopesByMaintenanceAppOrganization() {
        persistLink(maintenance, component);
        Organization otherOrg = persistOrganization("Org B", tenant);
        StatusApp otherApp = persistApp("App B", "app-b", tenant, otherOrg);
        StatusComponent otherComp = persistComponent("OtherComp", otherApp);
        StatusMaintenance otherM = persistMaintenance("OtherM", "SCHEDULED", otherApp);
        persistLink(otherM, otherComp);

        assertThat(repository.findByOrganizationId(organization.getId()))
                .extracting(mc -> mc.getComponent().getName()).containsExactly("API");
    }

    @Test
    void findByAppId_scopesByMaintenanceApp() {
        persistLink(maintenance, component);
        StatusApp otherApp = persistApp("App B", "app-b", tenant, organization);
        StatusComponent otherComp = persistComponent("OtherComp", otherApp);
        StatusMaintenance otherM = persistMaintenance("OtherM", "SCHEDULED", otherApp);
        persistLink(otherM, otherComp);

        assertThat(repository.findByAppId(app.getId()))
                .extracting(mc -> mc.getComponent().getName()).containsExactly("API");
    }

    @Test
    void countByMaintenanceId_countsAffectedComponents() {
        persistLink(maintenance, component);
        StatusComponent other = persistComponent("Web", app);
        persistLink(maintenance, other);

        assertThat(repository.countByMaintenanceId(maintenance.getId())).isEqualTo(2L);
    }

    @Test
    void countByComponentId_countsAffectingMaintenance() {
        persistLink(maintenance, component);
        StatusMaintenance other = persistMaintenance("Other", "SCHEDULED", app);
        persistLink(other, component);

        assertThat(repository.countByComponentId(component.getId())).isEqualTo(2L);
    }

    @Test
    void findActiveMaintenanceByComponentId_returnsScheduledOrInProgressOnly() {
        persistLink(maintenance, component); // SCHEDULED
        StatusMaintenance inProgress = persistMaintenance("InProgress", "IN_PROGRESS", app);
        persistLink(inProgress, component);
        StatusMaintenance completed = persistMaintenance("Completed", "COMPLETED", app);
        persistLink(completed, component);

        assertThat(repository.findActiveMaintenanceByComponentId(component.getId()))
                .extracting(mc -> mc.getMaintenance().getTitle())
                .containsExactlyInAnyOrder("Patch", "InProgress");
    }

    @Test
    void existsByMaintenanceIdAndComponentId_reflectsPresence() {
        persistLink(maintenance, component);
        StatusComponent other = persistComponent("Web", app);

        assertThat(repository.existsByMaintenanceIdAndComponentId(maintenance.getId(), component.getId())).isTrue();
        assertThat(repository.existsByMaintenanceIdAndComponentId(maintenance.getId(), other.getId())).isFalse();
    }

    @Test
    void deleteByMaintenanceId_removesAllLinksForMaintenance() {
        persistLink(maintenance, component);
        StatusComponent other = persistComponent("Web", app);
        persistLink(maintenance, other);

        repository.deleteByMaintenanceId(maintenance.getId());
        em.flush();
        em.clear();

        assertThat(repository.findByMaintenanceId(maintenance.getId())).isEmpty();
    }

    @Test
    void deleteByMaintenanceIdAndComponentId_removesSingleLink() {
        persistLink(maintenance, component);
        StatusComponent other = persistComponent("Web", app);
        persistLink(maintenance, other);

        repository.deleteByMaintenanceIdAndComponentId(maintenance.getId(), component.getId());
        em.flush();
        em.clear();

        assertThat(repository.findByMaintenanceId(maintenance.getId()))
                .extracting(mc -> mc.getComponent().getName()).containsExactly("Web");
    }
}
