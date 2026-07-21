package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusIncident;
import org.automatize.status.models.StatusIncidentUpdate;
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
 * {@code @DataJpaTest} coverage for {@link StatusIncidentUpdateRepository}'s
 * custom JPQL / derived queries against H2 (PostgreSQL compatibility mode).
 * Focuses on incident scoping, chronological ordering, status and date-range
 * filters, tenant/organization/app scoping, message search, counts and the
 * latest-update LIMIT query.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class StatusIncidentUpdateRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private StatusIncidentUpdateRepository repository;

    private Tenant tenant;
    private Organization organization;
    private StatusApp app;
    private StatusIncident incident;

    private final ZonedDateTime now = ZonedDateTime.now();

    @BeforeEach
    void setUp() {
        tenant = persistTenant("Tenant A");
        organization = persistOrganization("Org A", tenant);
        app = persistApp("App A", "app-a", tenant, organization);
        incident = persistIncident("Outage", app);
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

    private StatusIncident persistIncident(String title, StatusApp app) {
        StatusIncident i = new StatusIncident();
        i.setTitle(title);
        i.setStatus("INVESTIGATING");
        i.setSeverity("MAJOR");
        i.setStartedAt(now);
        i.setApp(app);
        i.setCreatedBy("test");
        i.setLastModifiedBy("test");
        return em.persistAndFlush(i);
    }

    private StatusIncidentUpdate persistUpdate(StatusIncident incident, String status,
                                               String message, ZonedDateTime updateTime) {
        StatusIncidentUpdate u = new StatusIncidentUpdate();
        u.setIncident(incident);
        u.setStatus(status);
        u.setMessage(message);
        u.setUpdateTime(updateTime);
        u.setCreatedBy("test");
        u.setLastModifiedBy("test");
        return em.persistAndFlush(u);
    }

    @Test
    void findByIncidentId_returnsUpdatesForIncident() {
        persistUpdate(incident, "INVESTIGATING", "looking into it", now);
        StatusIncident other = persistIncident("Other", app);
        persistUpdate(other, "IDENTIFIED", "root cause found", now);

        assertThat(repository.findByIncidentId(incident.getId()))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("looking into it");
    }

    @Test
    void findByIncidentIdOrderByUpdateTime_ordersOldestFirst() {
        persistUpdate(incident, "MONITORING", "third", now.plusHours(2));
        persistUpdate(incident, "INVESTIGATING", "first", now);
        persistUpdate(incident, "IDENTIFIED", "second", now.plusHours(1));

        assertThat(repository.findByIncidentIdOrderByUpdateTime(incident.getId()))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("first", "second", "third");
    }

    @Test
    void findByIncidentIdOrderByUpdateTimeDesc_ordersMostRecentFirst() {
        persistUpdate(incident, "INVESTIGATING", "first", now);
        persistUpdate(incident, "MONITORING", "third", now.plusHours(2));
        persistUpdate(incident, "IDENTIFIED", "second", now.plusHours(1));

        assertThat(repository.findByIncidentIdOrderByUpdateTimeDesc(incident.getId()))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("third", "second", "first");
    }

    @Test
    void findByStatus_filtersByStatus() {
        persistUpdate(incident, "INVESTIGATING", "a", now);
        persistUpdate(incident, "RESOLVED", "b", now);

        assertThat(repository.findByStatus("RESOLVED"))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("b");
    }

    @Test
    void findByUpdateTimeBetween_returnsUpdatesInRange() {
        persistUpdate(incident, "INVESTIGATING", "inRange", now);
        persistUpdate(incident, "INVESTIGATING", "outOfRange", now.minusDays(10));

        assertThat(repository.findByUpdateTimeBetween(now.minusHours(1), now.plusHours(1)))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("inRange");
    }

    @Test
    void findByTenantId_scopesByIncidentAppTenant() {
        persistUpdate(incident, "INVESTIGATING", "mine", now);
        Tenant other = persistTenant("Tenant B");
        Organization otherOrg = persistOrganization("Org B", other);
        StatusApp otherApp = persistApp("App B", "app-b", other, otherOrg);
        StatusIncident otherInc = persistIncident("OtherInc", otherApp);
        persistUpdate(otherInc, "INVESTIGATING", "theirs", now);

        assertThat(repository.findByTenantId(tenant.getId()))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("mine");
    }

    @Test
    void findByOrganizationId_scopesByIncidentAppOrganization() {
        persistUpdate(incident, "INVESTIGATING", "mine", now);
        Organization otherOrg = persistOrganization("Org B", tenant);
        StatusApp otherApp = persistApp("App B", "app-b", tenant, otherOrg);
        StatusIncident otherInc = persistIncident("OtherInc", otherApp);
        persistUpdate(otherInc, "INVESTIGATING", "theirs", now);

        assertThat(repository.findByOrganizationId(organization.getId()))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("mine");
    }

    @Test
    void findByAppId_scopesByIncidentApp() {
        persistUpdate(incident, "INVESTIGATING", "mine", now);
        StatusApp otherApp = persistApp("App B", "app-b", tenant, organization);
        StatusIncident otherInc = persistIncident("OtherInc", otherApp);
        persistUpdate(otherInc, "INVESTIGATING", "theirs", now);

        assertThat(repository.findByAppId(app.getId()))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("mine");
    }

    @Test
    void search_matchesMessageGlobally() {
        persistUpdate(incident, "INVESTIGATING", "database replica lag", now);
        persistUpdate(incident, "INVESTIGATING", "all clear", now);

        assertThat(repository.search("replica"))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("database replica lag");
    }

    @Test
    void searchByIncidentId_matchesMessageWithinIncident() {
        persistUpdate(incident, "INVESTIGATING", "database replica lag", now);
        StatusIncident other = persistIncident("Other", app);
        persistUpdate(other, "INVESTIGATING", "replica issue elsewhere", now);

        assertThat(repository.searchByIncidentId(incident.getId(), "replica"))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("database replica lag");
    }

    @Test
    void countByIncidentId_countsUpdates() {
        persistUpdate(incident, "INVESTIGATING", "a", now);
        persistUpdate(incident, "IDENTIFIED", "b", now.plusHours(1));

        assertThat(repository.countByIncidentId(incident.getId())).isEqualTo(2L);
    }

    @Test
    void findLatestByIncidentId_returnsMostRecentUpdate() {
        persistUpdate(incident, "INVESTIGATING", "first", now);
        persistUpdate(incident, "MONITORING", "latest", now.plusHours(2));
        persistUpdate(incident, "IDENTIFIED", "middle", now.plusHours(1));

        assertThat(repository.findLatestByIncidentId(incident.getId()))
                .extracting(StatusIncidentUpdate::getMessage).isEqualTo("latest");
    }
}
