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
class StatusIncidentUpdateRepositoryTest extends AbstractRepositoryTest {

    private static final String INVESTIGATING = "INVESTIGATING";
    private static final String IDENTIFIED = "IDENTIFIED";
    private static final String MONITORING = "MONITORING";
    private static final String THIRD = "third";
    private static final String FIRST = "first";
    private static final String SECOND = "second";
    private static final String APP_B = "App B";
    private static final String APP_B_SLUG = "app-b";
    private static final String OTHER_INC = "OtherInc";
    private static final String THEIRS = "theirs";
    private static final String DATABASE_REPLICA_LAG = "database replica lag";

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
        i.setStatus(INVESTIGATING);
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
        persistUpdate(incident, INVESTIGATING, "looking into it", now);
        StatusIncident other = persistIncident("Other", app);
        persistUpdate(other, IDENTIFIED, "root cause found", now);

        assertThat(repository.findByIncidentId(incident.getId()))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("looking into it");
    }

    @Test
    void findByIncidentIdOrderByUpdateTime_ordersOldestFirst() {
        persistUpdate(incident, MONITORING, THIRD, now.plusHours(2));
        persistUpdate(incident, INVESTIGATING, FIRST, now);
        persistUpdate(incident, IDENTIFIED, SECOND, now.plusHours(1));

        assertThat(repository.findByIncidentIdOrderByUpdateTime(incident.getId()))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly(FIRST, SECOND, THIRD);
    }

    @Test
    void findByIncidentIdOrderByUpdateTimeDesc_ordersMostRecentFirst() {
        persistUpdate(incident, INVESTIGATING, FIRST, now);
        persistUpdate(incident, MONITORING, THIRD, now.plusHours(2));
        persistUpdate(incident, IDENTIFIED, SECOND, now.plusHours(1));

        assertThat(repository.findByIncidentIdOrderByUpdateTimeDesc(incident.getId()))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly(THIRD, SECOND, FIRST);
    }

    @Test
    void findByStatus_filtersByStatus() {
        persistUpdate(incident, INVESTIGATING, "a", now);
        persistUpdate(incident, "RESOLVED", "b", now);

        assertThat(repository.findByStatus("RESOLVED"))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("b");
    }

    @Test
    void findByUpdateTimeBetween_returnsUpdatesInRange() {
        persistUpdate(incident, INVESTIGATING, "inRange", now);
        persistUpdate(incident, INVESTIGATING, "outOfRange", now.minusDays(10));

        assertThat(repository.findByUpdateTimeBetween(now.minusHours(1), now.plusHours(1)))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("inRange");
    }

    @Test
    void findByTenantId_scopesByIncidentAppTenant() {
        persistUpdate(incident, INVESTIGATING, "mine", now);
        Tenant other = persistTenant("Tenant B");
        Organization otherOrg = persistOrganization("Org B", other);
        StatusApp otherApp = persistApp(APP_B, APP_B_SLUG, other, otherOrg);
        StatusIncident otherInc = persistIncident(OTHER_INC, otherApp);
        persistUpdate(otherInc, INVESTIGATING, THEIRS, now);

        assertThat(repository.findByTenantId(tenant.getId()))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("mine");
    }

    @Test
    void findByOrganizationId_scopesByIncidentAppOrganization() {
        persistUpdate(incident, INVESTIGATING, "mine", now);
        Organization otherOrg = persistOrganization("Org B", tenant);
        StatusApp otherApp = persistApp(APP_B, APP_B_SLUG, tenant, otherOrg);
        StatusIncident otherInc = persistIncident(OTHER_INC, otherApp);
        persistUpdate(otherInc, INVESTIGATING, THEIRS, now);

        assertThat(repository.findByOrganizationId(organization.getId()))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("mine");
    }

    @Test
    void findByAppId_scopesByIncidentApp() {
        persistUpdate(incident, INVESTIGATING, "mine", now);
        StatusApp otherApp = persistApp(APP_B, APP_B_SLUG, tenant, organization);
        StatusIncident otherInc = persistIncident(OTHER_INC, otherApp);
        persistUpdate(otherInc, INVESTIGATING, THEIRS, now);

        assertThat(repository.findByAppId(app.getId()))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly("mine");
    }

    @Test
    void search_matchesMessageGlobally() {
        persistUpdate(incident, INVESTIGATING, DATABASE_REPLICA_LAG, now);
        persistUpdate(incident, INVESTIGATING, "all clear", now);

        assertThat(repository.search("replica"))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly(DATABASE_REPLICA_LAG);
    }

    @Test
    void searchByIncidentId_matchesMessageWithinIncident() {
        persistUpdate(incident, INVESTIGATING, DATABASE_REPLICA_LAG, now);
        StatusIncident other = persistIncident("Other", app);
        persistUpdate(other, INVESTIGATING, "replica issue elsewhere", now);

        assertThat(repository.searchByIncidentId(incident.getId(), "replica"))
                .extracting(StatusIncidentUpdate::getMessage).containsExactly(DATABASE_REPLICA_LAG);
    }

    @Test
    void countByIncidentId_countsUpdates() {
        persistUpdate(incident, INVESTIGATING, "a", now);
        persistUpdate(incident, IDENTIFIED, "b", now.plusHours(1));

        assertThat(repository.countByIncidentId(incident.getId())).isEqualTo(2L);
    }

    @Test
    void findLatestByIncidentId_returnsMostRecentUpdate() {
        persistUpdate(incident, INVESTIGATING, FIRST, now);
        persistUpdate(incident, MONITORING, "latest", now.plusHours(2));
        persistUpdate(incident, IDENTIFIED, "middle", now.plusHours(1));

        assertThat(repository.findLatestByIncidentId(incident.getId()))
                .extracting(StatusIncidentUpdate::getMessage).isEqualTo("latest");
    }
}
