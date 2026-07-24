package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.StatusIncident;
import org.automatize.status.models.StatusIncidentComponent;
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
 * {@code @DataJpaTest} coverage for {@link StatusIncidentComponentRepository}'s
 * custom JPQL / derived queries against H2 (PostgreSQL compatibility mode).
 * Focuses on incident/component association lookups, status filters,
 * tenant/organization/app scoping (deep path traversal), counts, active-incident
 * queries, existence checks and derived deletes.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class StatusIncidentComponentRepositoryTest extends AbstractAppScopedRepositoryTest {

    @Autowired
    private StatusIncidentComponentRepository repository;

    private StatusComponent component;
    private StatusIncident incident;

    private static final String STATUS_INVESTIGATING = "INVESTIGATING";
    private static final String STATUS_DEGRADED = "DEGRADED";
    private static final String STATUS_OUTAGE = "OUTAGE";
    private static final String APP_B_NAME = "App B";
    private static final String APP_B_SLUG = "app-b";
    private static final String OTHER_COMPONENT_NAME = "OtherComp";
    private static final String OTHER_INCIDENT_TITLE = "OtherInc";

    private final ZonedDateTime now = ZonedDateTime.now();

    @BeforeEach
    void setUp() {
        component = persistComponent("API", app);
        incident = persistIncident("Outage", STATUS_INVESTIGATING, app);
    }

    private StatusIncident persistIncident(String title, String status, StatusApp app) {
        StatusIncident i = new StatusIncident();
        i.setTitle(title);
        i.setStatus(status);
        i.setSeverity("MAJOR");
        i.setStartedAt(now);
        i.setApp(app);
        i.setCreatedBy("test");
        i.setLastModifiedBy("test");
        return em.persistAndFlush(i);
    }

    private StatusIncidentComponent persistLink(StatusIncident incident, StatusComponent component, String status) {
        StatusIncidentComponent ic = new StatusIncidentComponent();
        ic.setIncident(incident);
        ic.setComponent(component);
        ic.setComponentStatus(status);
        return em.persistAndFlush(ic);
    }

    @Test
    void findByIncidentId_returnsLinksForIncident() {
        persistLink(incident, component, STATUS_DEGRADED);
        StatusIncident other = persistIncident("Other", STATUS_INVESTIGATING, app);
        persistLink(other, component, STATUS_OUTAGE);

        assertThat(repository.findByIncidentId(incident.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly(STATUS_DEGRADED);
    }

    @Test
    void findByComponentId_returnsLinksForComponent() {
        persistLink(incident, component, STATUS_DEGRADED);
        StatusComponent other = persistComponent("Web", app);
        persistLink(incident, other, STATUS_OUTAGE);

        assertThat(repository.findByComponentId(component.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly(STATUS_DEGRADED);
    }

    @Test
    void findByIncidentIdAndComponentId_returnsSpecificLink() {
        persistLink(incident, component, STATUS_DEGRADED);

        assertThat(repository.findByIncidentIdAndComponentId(incident.getId(), component.getId()))
                .isPresent()
                .get().extracting(StatusIncidentComponent::getComponentStatus).isEqualTo(STATUS_DEGRADED);
        StatusComponent other = persistComponent("Web", app);
        assertThat(repository.findByIncidentIdAndComponentId(incident.getId(), other.getId())).isEmpty();
    }

    @Test
    void findByComponentStatus_filtersByStatus() {
        persistLink(incident, component, STATUS_DEGRADED);
        StatusComponent other = persistComponent("Web", app);
        persistLink(incident, other, STATUS_OUTAGE);

        assertThat(repository.findByComponentStatus(STATUS_OUTAGE))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly(STATUS_OUTAGE);
    }

    @Test
    void findByIncidentIdAndComponentStatus_combinesIncidentAndStatus() {
        persistLink(incident, component, STATUS_DEGRADED);
        StatusComponent other = persistComponent("Web", app);
        persistLink(incident, other, STATUS_OUTAGE);

        assertThat(repository.findByIncidentIdAndComponentStatus(incident.getId(), STATUS_OUTAGE))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly(STATUS_OUTAGE);
    }

    @Test
    void findByTenantId_scopesByIncidentAppTenant() {
        persistLink(incident, component, STATUS_DEGRADED);
        Tenant other = persistTenant("Tenant B");
        Organization otherOrg = persistOrganization("Org B", other);
        StatusApp otherApp = persistApp(APP_B_NAME, APP_B_SLUG, other, otherOrg);
        StatusComponent otherComp = persistComponent(OTHER_COMPONENT_NAME, otherApp);
        StatusIncident otherInc = persistIncident(OTHER_INCIDENT_TITLE, STATUS_INVESTIGATING, otherApp);
        persistLink(otherInc, otherComp, STATUS_OUTAGE);

        assertThat(repository.findByTenantId(tenant.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly(STATUS_DEGRADED);
    }

    @Test
    void findByOrganizationId_scopesByIncidentAppOrganization() {
        persistLink(incident, component, STATUS_DEGRADED);
        Organization otherOrg = persistOrganization("Org B", tenant);
        StatusApp otherApp = persistApp(APP_B_NAME, APP_B_SLUG, tenant, otherOrg);
        StatusComponent otherComp = persistComponent(OTHER_COMPONENT_NAME, otherApp);
        StatusIncident otherInc = persistIncident(OTHER_INCIDENT_TITLE, STATUS_INVESTIGATING, otherApp);
        persistLink(otherInc, otherComp, STATUS_OUTAGE);

        assertThat(repository.findByOrganizationId(organization.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly(STATUS_DEGRADED);
    }

    @Test
    void findByAppId_scopesByIncidentApp() {
        persistLink(incident, component, STATUS_DEGRADED);
        StatusApp otherApp = persistApp(APP_B_NAME, APP_B_SLUG, tenant, organization);
        StatusComponent otherComp = persistComponent(OTHER_COMPONENT_NAME, otherApp);
        StatusIncident otherInc = persistIncident(OTHER_INCIDENT_TITLE, STATUS_INVESTIGATING, otherApp);
        persistLink(otherInc, otherComp, STATUS_OUTAGE);

        assertThat(repository.findByAppId(app.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly(STATUS_DEGRADED);
    }

    @Test
    void countByIncidentId_countsAffectedComponents() {
        persistLink(incident, component, STATUS_DEGRADED);
        StatusComponent other = persistComponent("Web", app);
        persistLink(incident, other, STATUS_OUTAGE);

        assertThat(repository.countByIncidentId(incident.getId())).isEqualTo(2L);
    }

    @Test
    void countByComponentId_countsAffectingIncidents() {
        persistLink(incident, component, STATUS_DEGRADED);
        StatusIncident other = persistIncident("Other", STATUS_INVESTIGATING, app);
        persistLink(other, component, STATUS_OUTAGE);

        assertThat(repository.countByComponentId(component.getId())).isEqualTo(2L);
    }

    @Test
    void findActiveIncidentsByComponentId_excludesResolvedIncidents() {
        persistLink(incident, component, STATUS_DEGRADED);
        StatusIncident resolved = persistIncident("Resolved", "RESOLVED", app);
        persistLink(resolved, component, STATUS_OUTAGE);

        assertThat(repository.findActiveIncidentsByComponentId(component.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly(STATUS_DEGRADED);
    }

    @Test
    void existsByIncidentIdAndComponentId_reflectsPresence() {
        persistLink(incident, component, STATUS_DEGRADED);
        StatusComponent other = persistComponent("Web", app);

        assertThat(repository.existsByIncidentIdAndComponentId(incident.getId(), component.getId())).isTrue();
        assertThat(repository.existsByIncidentIdAndComponentId(incident.getId(), other.getId())).isFalse();
    }

    @Test
    void deleteByIncidentId_removesAllLinksForIncident() {
        persistLink(incident, component, STATUS_DEGRADED);
        StatusComponent other = persistComponent("Web", app);
        persistLink(incident, other, STATUS_OUTAGE);

        repository.deleteByIncidentId(incident.getId());
        em.flush();
        em.clear();

        assertThat(repository.findByIncidentId(incident.getId())).isEmpty();
    }

    @Test
    void deleteByIncidentIdAndComponentId_removesSingleLink() {
        persistLink(incident, component, STATUS_DEGRADED);
        StatusComponent other = persistComponent("Web", app);
        persistLink(incident, other, STATUS_OUTAGE);

        repository.deleteByIncidentIdAndComponentId(incident.getId(), component.getId());
        em.flush();
        em.clear();

        assertThat(repository.findByIncidentId(incident.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly(STATUS_OUTAGE);
    }

    @Test
    void findPublicIncidentsAffectingComponentOnDate_includesStartedOnDayAndOngoing() {
        ZonedDateTime dayStart = now.toLocalDate().atStartOfDay(now.getZone());
        ZonedDateTime dayEnd = dayStart.plusDays(1).minusSeconds(1);

        StatusIncident startedToday = persistIncident("StartedToday", STATUS_INVESTIGATING, app);
        startedToday.setStartedAt(dayStart.plusHours(1));
        em.persistAndFlush(startedToday);
        persistLink(startedToday, component, STATUS_DEGRADED);

        StatusIncident ongoing = persistIncident("Ongoing", STATUS_INVESTIGATING, app);
        ongoing.setStartedAt(dayStart.minusDays(2));
        em.persistAndFlush(ongoing);
        persistLink(ongoing, component, STATUS_OUTAGE);

        StatusIncident resolvedBefore = persistIncident("ResolvedBefore", "RESOLVED", app);
        resolvedBefore.setStartedAt(dayStart.minusDays(3));
        resolvedBefore.setResolvedAt(dayStart.minusDays(2));
        em.persistAndFlush(resolvedBefore);
        persistLink(resolvedBefore, component, STATUS_OUTAGE);

        StatusIncident privateToday = persistIncident("PrivateToday", STATUS_INVESTIGATING, app);
        privateToday.setStartedAt(dayStart.plusHours(2));
        privateToday.setIsPublic(false);
        em.persistAndFlush(privateToday);
        persistLink(privateToday, component, STATUS_DEGRADED);

        assertThat(repository.findPublicIncidentsAffectingComponentOnDate(component.getId(), dayStart, dayEnd))
                .extracting(ic -> ic.getIncident().getTitle())
                .containsExactlyInAnyOrder("StartedToday", "Ongoing");
    }
}
