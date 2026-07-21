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
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
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
class StatusIncidentComponentRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private StatusIncidentComponentRepository repository;

    private Tenant tenant;
    private Organization organization;
    private StatusApp app;
    private StatusComponent component;
    private StatusIncident incident;

    private final ZonedDateTime now = ZonedDateTime.now();

    @BeforeEach
    void setUp() {
        tenant = persistTenant("Tenant A");
        organization = persistOrganization("Org A", tenant);
        app = persistApp("App A", "app-a", tenant, organization);
        component = persistComponent("API", app);
        incident = persistIncident("Outage", "INVESTIGATING", app);
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
        persistLink(incident, component, "DEGRADED");
        StatusIncident other = persistIncident("Other", "INVESTIGATING", app);
        persistLink(other, component, "OUTAGE");

        assertThat(repository.findByIncidentId(incident.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly("DEGRADED");
    }

    @Test
    void findByComponentId_returnsLinksForComponent() {
        persistLink(incident, component, "DEGRADED");
        StatusComponent other = persistComponent("Web", app);
        persistLink(incident, other, "OUTAGE");

        assertThat(repository.findByComponentId(component.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly("DEGRADED");
    }

    @Test
    void findByIncidentIdAndComponentId_returnsSpecificLink() {
        persistLink(incident, component, "DEGRADED");

        assertThat(repository.findByIncidentIdAndComponentId(incident.getId(), component.getId()))
                .isPresent()
                .get().extracting(StatusIncidentComponent::getComponentStatus).isEqualTo("DEGRADED");
        StatusComponent other = persistComponent("Web", app);
        assertThat(repository.findByIncidentIdAndComponentId(incident.getId(), other.getId())).isEmpty();
    }

    @Test
    void findByComponentStatus_filtersByStatus() {
        persistLink(incident, component, "DEGRADED");
        StatusComponent other = persistComponent("Web", app);
        persistLink(incident, other, "OUTAGE");

        assertThat(repository.findByComponentStatus("OUTAGE"))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly("OUTAGE");
    }

    @Test
    void findByIncidentIdAndComponentStatus_combinesIncidentAndStatus() {
        persistLink(incident, component, "DEGRADED");
        StatusComponent other = persistComponent("Web", app);
        persistLink(incident, other, "OUTAGE");

        assertThat(repository.findByIncidentIdAndComponentStatus(incident.getId(), "OUTAGE"))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly("OUTAGE");
    }

    @Test
    void findByTenantId_scopesByIncidentAppTenant() {
        persistLink(incident, component, "DEGRADED");
        Tenant other = persistTenant("Tenant B");
        Organization otherOrg = persistOrganization("Org B", other);
        StatusApp otherApp = persistApp("App B", "app-b", other, otherOrg);
        StatusComponent otherComp = persistComponent("OtherComp", otherApp);
        StatusIncident otherInc = persistIncident("OtherInc", "INVESTIGATING", otherApp);
        persistLink(otherInc, otherComp, "OUTAGE");

        assertThat(repository.findByTenantId(tenant.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly("DEGRADED");
    }

    @Test
    void findByOrganizationId_scopesByIncidentAppOrganization() {
        persistLink(incident, component, "DEGRADED");
        Organization otherOrg = persistOrganization("Org B", tenant);
        StatusApp otherApp = persistApp("App B", "app-b", tenant, otherOrg);
        StatusComponent otherComp = persistComponent("OtherComp", otherApp);
        StatusIncident otherInc = persistIncident("OtherInc", "INVESTIGATING", otherApp);
        persistLink(otherInc, otherComp, "OUTAGE");

        assertThat(repository.findByOrganizationId(organization.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly("DEGRADED");
    }

    @Test
    void findByAppId_scopesByIncidentApp() {
        persistLink(incident, component, "DEGRADED");
        StatusApp otherApp = persistApp("App B", "app-b", tenant, organization);
        StatusComponent otherComp = persistComponent("OtherComp", otherApp);
        StatusIncident otherInc = persistIncident("OtherInc", "INVESTIGATING", otherApp);
        persistLink(otherInc, otherComp, "OUTAGE");

        assertThat(repository.findByAppId(app.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly("DEGRADED");
    }

    @Test
    void countByIncidentId_countsAffectedComponents() {
        persistLink(incident, component, "DEGRADED");
        StatusComponent other = persistComponent("Web", app);
        persistLink(incident, other, "OUTAGE");

        assertThat(repository.countByIncidentId(incident.getId())).isEqualTo(2L);
    }

    @Test
    void countByComponentId_countsAffectingIncidents() {
        persistLink(incident, component, "DEGRADED");
        StatusIncident other = persistIncident("Other", "INVESTIGATING", app);
        persistLink(other, component, "OUTAGE");

        assertThat(repository.countByComponentId(component.getId())).isEqualTo(2L);
    }

    @Test
    void findActiveIncidentsByComponentId_excludesResolvedIncidents() {
        persistLink(incident, component, "DEGRADED");
        StatusIncident resolved = persistIncident("Resolved", "RESOLVED", app);
        persistLink(resolved, component, "OUTAGE");

        assertThat(repository.findActiveIncidentsByComponentId(component.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly("DEGRADED");
    }

    @Test
    void existsByIncidentIdAndComponentId_reflectsPresence() {
        persistLink(incident, component, "DEGRADED");
        StatusComponent other = persistComponent("Web", app);

        assertThat(repository.existsByIncidentIdAndComponentId(incident.getId(), component.getId())).isTrue();
        assertThat(repository.existsByIncidentIdAndComponentId(incident.getId(), other.getId())).isFalse();
    }

    @Test
    void deleteByIncidentId_removesAllLinksForIncident() {
        persistLink(incident, component, "DEGRADED");
        StatusComponent other = persistComponent("Web", app);
        persistLink(incident, other, "OUTAGE");

        repository.deleteByIncidentId(incident.getId());
        em.flush();
        em.clear();

        assertThat(repository.findByIncidentId(incident.getId())).isEmpty();
    }

    @Test
    void deleteByIncidentIdAndComponentId_removesSingleLink() {
        persistLink(incident, component, "DEGRADED");
        StatusComponent other = persistComponent("Web", app);
        persistLink(incident, other, "OUTAGE");

        repository.deleteByIncidentIdAndComponentId(incident.getId(), component.getId());
        em.flush();
        em.clear();

        assertThat(repository.findByIncidentId(incident.getId()))
                .extracting(StatusIncidentComponent::getComponentStatus).containsExactly("OUTAGE");
    }

    @Test
    void findPublicIncidentsAffectingComponentOnDate_includesStartedOnDayAndOngoing() {
        ZonedDateTime dayStart = now.toLocalDate().atStartOfDay(now.getZone());
        ZonedDateTime dayEnd = dayStart.plusDays(1).minusSeconds(1);

        StatusIncident startedToday = persistIncident("StartedToday", "INVESTIGATING", app);
        startedToday.setStartedAt(dayStart.plusHours(1));
        em.persistAndFlush(startedToday);
        persistLink(startedToday, component, "DEGRADED");

        StatusIncident ongoing = persistIncident("Ongoing", "INVESTIGATING", app);
        ongoing.setStartedAt(dayStart.minusDays(2));
        em.persistAndFlush(ongoing);
        persistLink(ongoing, component, "OUTAGE");

        StatusIncident resolvedBefore = persistIncident("ResolvedBefore", "RESOLVED", app);
        resolvedBefore.setStartedAt(dayStart.minusDays(3));
        resolvedBefore.setResolvedAt(dayStart.minusDays(2));
        em.persistAndFlush(resolvedBefore);
        persistLink(resolvedBefore, component, "OUTAGE");

        StatusIncident privateToday = persistIncident("PrivateToday", "INVESTIGATING", app);
        privateToday.setStartedAt(dayStart.plusHours(2));
        privateToday.setIsPublic(false);
        em.persistAndFlush(privateToday);
        persistLink(privateToday, component, "DEGRADED");

        assertThat(repository.findPublicIncidentsAffectingComponentOnDate(component.getId(), dayStart, dayEnd))
                .extracting(ic -> ic.getIncident().getTitle())
                .containsExactlyInAnyOrder("StartedToday", "Ongoing");
    }
}
