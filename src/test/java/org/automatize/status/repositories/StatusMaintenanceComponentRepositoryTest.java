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
class StatusMaintenanceComponentRepositoryTest extends AbstractAppScopedRepositoryTest {

    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String TITLE_OTHER = "Other";
    private static final String APP_B_NAME = "App B";
    private static final String APP_B_SLUG = "app-b";
    private static final String OTHER_COMP = "OtherComp";
    private static final String OTHER_M = "OtherM";

    @Autowired
    private StatusMaintenanceComponentRepository repository;

    private StatusComponent component;
    private StatusMaintenance maintenance;

    private final ZonedDateTime now = ZonedDateTime.now();

    @BeforeEach
    void setUp() {
        component = persistComponent("API", app);
        maintenance = persistMaintenance("Patch", STATUS_SCHEDULED, app);
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
        StatusMaintenance other = persistMaintenance(TITLE_OTHER, STATUS_SCHEDULED, app);
        StatusComponent otherComp = persistComponent("Web", app);
        persistLink(other, otherComp);

        assertThat(repository.findByMaintenanceId(maintenance.getId()))
                .extracting(mc -> mc.getComponent().getName()).containsExactly("API");
    }

    @Test
    void findByComponentId_returnsLinksForComponent() {
        persistLink(maintenance, component);
        StatusMaintenance other = persistMaintenance(TITLE_OTHER, STATUS_SCHEDULED, app);
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
        StatusApp otherApp = persistApp(APP_B_NAME, APP_B_SLUG, other, otherOrg);
        StatusComponent otherComp = persistComponent(OTHER_COMP, otherApp);
        StatusMaintenance otherM = persistMaintenance(OTHER_M, STATUS_SCHEDULED, otherApp);
        persistLink(otherM, otherComp);

        assertThat(repository.findByTenantId(tenant.getId()))
                .extracting(mc -> mc.getComponent().getName()).containsExactly("API");
    }

    @Test
    void findByOrganizationId_scopesByMaintenanceAppOrganization() {
        persistLink(maintenance, component);
        Organization otherOrg = persistOrganization("Org B", tenant);
        StatusApp otherApp = persistApp(APP_B_NAME, APP_B_SLUG, tenant, otherOrg);
        StatusComponent otherComp = persistComponent(OTHER_COMP, otherApp);
        StatusMaintenance otherM = persistMaintenance(OTHER_M, STATUS_SCHEDULED, otherApp);
        persistLink(otherM, otherComp);

        assertThat(repository.findByOrganizationId(organization.getId()))
                .extracting(mc -> mc.getComponent().getName()).containsExactly("API");
    }

    @Test
    void findByAppId_scopesByMaintenanceApp() {
        persistLink(maintenance, component);
        StatusApp otherApp = persistApp(APP_B_NAME, APP_B_SLUG, tenant, organization);
        StatusComponent otherComp = persistComponent(OTHER_COMP, otherApp);
        StatusMaintenance otherM = persistMaintenance(OTHER_M, STATUS_SCHEDULED, otherApp);
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
        StatusMaintenance other = persistMaintenance(TITLE_OTHER, STATUS_SCHEDULED, app);
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
