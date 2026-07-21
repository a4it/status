package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusIncident;
import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} coverage for {@link StatusIncidentRepository}'s custom JPQL /
 * derived queries against H2 (PostgreSQL compatibility mode). Focuses on app /
 * tenant / organization scoping, status / severity / visibility filters,
 * date-range and resolved-state queries, ordering, search, counts and the
 * public status page bulk / date-window queries.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class StatusIncidentRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private StatusIncidentRepository repository;

    private Tenant tenant;
    private Organization organization;
    private StatusApp app;

    private final ZonedDateTime now = ZonedDateTime.now();

    @BeforeEach
    void setUp() {
        tenant = persistTenant("Tenant A");
        organization = persistOrganization("Org A", tenant);
        app = persistApp("App A", "app-a", tenant, organization);
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

    private StatusIncident persistIncident(String title, String status, String severity,
                                           ZonedDateTime startedAt, StatusApp app) {
        StatusIncident i = new StatusIncident();
        i.setTitle(title);
        i.setStatus(status);
        i.setSeverity(severity);
        i.setStartedAt(startedAt);
        i.setApp(app);
        i.setCreatedBy("test");
        i.setLastModifiedBy("test");
        return em.persistAndFlush(i);
    }

    @Test
    void findByAppId_returnsIncidentsOfApp() {
        persistIncident("Outage", "INVESTIGATING", "MAJOR", now, app);
        StatusApp other = persistApp("App B", "app-b", tenant, organization);
        persistIncident("Other", "INVESTIGATING", "MINOR", now, other);

        assertThat(repository.findByAppId(app.getId()))
                .extracting(StatusIncident::getTitle).containsExactly("Outage");
    }

    @Test
    void findByStatus_filtersByStatus() {
        persistIncident("Open", "INVESTIGATING", "MAJOR", now, app);
        persistIncident("Done", "RESOLVED", "MAJOR", now, app);

        assertThat(repository.findByStatus("RESOLVED"))
                .extracting(StatusIncident::getTitle).containsExactly("Done");
    }

    @Test
    void findBySeverity_filtersBySeverity() {
        persistIncident("Critical", "INVESTIGATING", "CRITICAL", now, app);
        persistIncident("Minor", "INVESTIGATING", "MINOR", now, app);

        assertThat(repository.findBySeverity("CRITICAL"))
                .extracting(StatusIncident::getTitle).containsExactly("Critical");
    }

    @Test
    void findByIsPublic_filtersByVisibility() {
        StatusIncident priv = persistIncident("Private", "INVESTIGATING", "MAJOR", now, app);
        priv.setIsPublic(false);
        em.persistAndFlush(priv);
        persistIncident("Public", "INVESTIGATING", "MAJOR", now, app);

        assertThat(repository.findByIsPublic(false))
                .extracting(StatusIncident::getTitle).containsExactly("Private");
    }

    @Test
    void findByAppIdAndStatus_combinesAppAndStatus() {
        persistIncident("Open", "INVESTIGATING", "MAJOR", now, app);
        persistIncident("Done", "RESOLVED", "MAJOR", now, app);

        assertThat(repository.findByAppIdAndStatus(app.getId(), "INVESTIGATING"))
                .extracting(StatusIncident::getTitle).containsExactly("Open");
    }

    @Test
    void findByAppIdAndStatusNot_excludesGivenStatus() {
        persistIncident("Open", "INVESTIGATING", "MAJOR", now, app);
        persistIncident("Done", "RESOLVED", "MAJOR", now, app);

        assertThat(repository.findByAppIdAndStatusNot(app.getId(), "RESOLVED"))
                .extracting(StatusIncident::getTitle).containsExactly("Open");
    }

    @Test
    void findActivePublicIncidentsByAppIdIn_returnsOnlyActivePublic() {
        persistIncident("ActivePublic", "INVESTIGATING", "MAJOR", now, app);
        persistIncident("Resolved", "RESOLVED", "MAJOR", now, app);
        StatusIncident priv = persistIncident("ActivePrivate", "INVESTIGATING", "MAJOR", now, app);
        priv.setIsPublic(false);
        em.persistAndFlush(priv);

        assertThat(repository.findActivePublicIncidentsByAppIdIn(List.of(app.getId())))
                .extracting(StatusIncident::getTitle).containsExactly("ActivePublic");
    }

    @Test
    void findByAppIdAndIsPublic_combinesAppAndVisibility() {
        persistIncident("Public", "INVESTIGATING", "MAJOR", now, app);
        StatusIncident priv = persistIncident("Private", "INVESTIGATING", "MAJOR", now, app);
        priv.setIsPublic(false);
        em.persistAndFlush(priv);

        assertThat(repository.findByAppIdAndIsPublic(app.getId(), true))
                .extracting(StatusIncident::getTitle).containsExactly("Public");
    }

    @Test
    void findByStartedAtBetween_returnsIncidentsInRange() {
        persistIncident("InRange", "INVESTIGATING", "MAJOR", now.minusHours(1), app);
        persistIncident("OutOfRange", "INVESTIGATING", "MAJOR", now.minusDays(10), app);

        List<StatusIncident> result =
                repository.findByStartedAtBetween(now.minusHours(2), now.plusHours(1));

        assertThat(result).extracting(StatusIncident::getTitle).containsExactly("InRange");
    }

    @Test
    void findByResolvedAtIsNull_returnsUnresolvedIncidents() {
        persistIncident("Ongoing", "INVESTIGATING", "MAJOR", now, app);
        StatusIncident resolved = persistIncident("Closed", "RESOLVED", "MAJOR", now, app);
        resolved.setResolvedAt(now);
        em.persistAndFlush(resolved);

        assertThat(repository.findByResolvedAtIsNull())
                .extracting(StatusIncident::getTitle).containsExactly("Ongoing");
    }

    @Test
    void findByResolvedAtIsNotNull_returnsResolvedIncidents() {
        persistIncident("Ongoing", "INVESTIGATING", "MAJOR", now, app);
        StatusIncident resolved = persistIncident("Closed", "RESOLVED", "MAJOR", now, app);
        resolved.setResolvedAt(now);
        em.persistAndFlush(resolved);

        assertThat(repository.findByResolvedAtIsNotNull())
                .extracting(StatusIncident::getTitle).containsExactly("Closed");
    }

    @Test
    void findByTenantId_scopesByAppTenant() {
        persistIncident("Mine", "INVESTIGATING", "MAJOR", now, app);
        Tenant other = persistTenant("Tenant B");
        Organization otherOrg = persistOrganization("Org B", other);
        StatusApp otherApp = persistApp("App B", "app-b", other, otherOrg);
        persistIncident("Theirs", "INVESTIGATING", "MAJOR", now, otherApp);

        assertThat(repository.findByTenantId(tenant.getId()))
                .extracting(StatusIncident::getTitle).containsExactly("Mine");
    }

    @Test
    void findByOrganizationId_scopesByAppOrganization() {
        persistIncident("Mine", "INVESTIGATING", "MAJOR", now, app);
        Organization otherOrg = persistOrganization("Org B", tenant);
        StatusApp otherApp = persistApp("App B", "app-b", tenant, otherOrg);
        persistIncident("Theirs", "INVESTIGATING", "MAJOR", now, otherApp);

        assertThat(repository.findByOrganizationId(organization.getId()))
                .extracting(StatusIncident::getTitle).containsExactly("Mine");
    }

    @Test
    void findByAppIdOrderByStartedAtDesc_ordersMostRecentFirst() {
        persistIncident("Old", "INVESTIGATING", "MAJOR", now.minusDays(2), app);
        persistIncident("New", "INVESTIGATING", "MAJOR", now, app);
        persistIncident("Mid", "INVESTIGATING", "MAJOR", now.minusDays(1), app);

        assertThat(repository.findByAppIdOrderByStartedAtDesc(app.getId()))
                .extracting(StatusIncident::getTitle).containsExactly("New", "Mid", "Old");
    }

    @Test
    void findRecentIncidentsByAppId_returnsThoseOnOrAfterDate() {
        persistIncident("Recent", "INVESTIGATING", "MAJOR", now.minusHours(1), app);
        persistIncident("Ancient", "INVESTIGATING", "MAJOR", now.minusDays(30), app);

        assertThat(repository.findRecentIncidentsByAppId(app.getId(), now.minusDays(7)))
                .extracting(StatusIncident::getTitle).containsExactly("Recent");
    }

    @Test
    void search_matchesTitleOrDescription() {
        StatusIncident i = persistIncident("Database outage", "INVESTIGATING", "MAJOR", now, app);
        i.setDescription("replica lag detected");
        em.persistAndFlush(i);
        persistIncident("Network blip", "INVESTIGATING", "MINOR", now, app);

        assertThat(repository.search("outage"))
                .extracting(StatusIncident::getTitle).containsExactly("Database outage");
        assertThat(repository.search("replica"))
                .extracting(StatusIncident::getTitle).containsExactly("Database outage");
    }

    @Test
    void countByAppId_countsIncidents() {
        persistIncident("A", "INVESTIGATING", "MAJOR", now, app);
        persistIncident("B", "RESOLVED", "MAJOR", now, app);

        assertThat(repository.countByAppId(app.getId())).isEqualTo(2L);
    }

    @Test
    void countByTenantId_countsScopedIncidents() {
        persistIncident("A", "INVESTIGATING", "MAJOR", now, app);
        Tenant other = persistTenant("Tenant B");
        Organization otherOrg = persistOrganization("Org B", other);
        StatusApp otherApp = persistApp("App B", "app-b", other, otherOrg);
        persistIncident("B", "INVESTIGATING", "MAJOR", now, otherApp);

        assertThat(repository.countByTenantId(tenant.getId())).isEqualTo(1L);
    }

    @Test
    void countActiveIncidentsByAppId_excludesResolved() {
        persistIncident("Open", "INVESTIGATING", "MAJOR", now, app);
        persistIncident("Done", "RESOLVED", "MAJOR", now, app);

        assertThat(repository.countActiveIncidentsByAppId(app.getId())).isEqualTo(1L);
    }

    @Test
    void findActiveAutomatedIncidents_filtersByCreatorAndActiveStatus() {
        StatusIncident systemActive = persistIncident("Auto", "INVESTIGATING", "MAJOR", now, app);
        systemActive.setCreatedBy("system");
        em.persistAndFlush(systemActive);

        StatusIncident systemResolved = persistIncident("AutoDone", "RESOLVED", "MAJOR", now, app);
        systemResolved.setCreatedBy("system");
        em.persistAndFlush(systemResolved);

        // Human-created active incident should be excluded.
        persistIncident("Manual", "INVESTIGATING", "MAJOR", now, app);

        assertThat(repository.findActiveAutomatedIncidents(app.getId(), "system"))
                .extracting(StatusIncident::getTitle).containsExactly("Auto");
    }

    @Test
    void findRecentPublicIncidentsByAppId_returnsRecentPublicOnly() {
        persistIncident("RecentPublic", "INVESTIGATING", "MAJOR", now.minusHours(1), app);
        StatusIncident priv = persistIncident("RecentPrivate", "INVESTIGATING", "MAJOR", now.minusHours(1), app);
        priv.setIsPublic(false);
        em.persistAndFlush(priv);
        persistIncident("OldPublic", "INVESTIGATING", "MAJOR", now.minusDays(30), app);

        assertThat(repository.findRecentPublicIncidentsByAppId(app.getId(), now.minusDays(7)))
                .extracting(StatusIncident::getTitle).containsExactly("RecentPublic");
    }

    @Test
    void findPublicIncidentsAffectingDate_includesStartedOnDayAndOngoingFromBefore() {
        ZonedDateTime dayStart = now.toLocalDate().atStartOfDay(now.getZone());
        ZonedDateTime dayEnd = dayStart.plusDays(1).minusSeconds(1);

        // Started on the day.
        persistIncident("StartedToday", "INVESTIGATING", "MAJOR", dayStart.plusHours(2), app);
        // Started before, still unresolved -> ongoing.
        persistIncident("OngoingFromBefore", "INVESTIGATING", "MAJOR", dayStart.minusDays(2), app);
        // Started before and resolved before the day -> excluded.
        StatusIncident old = persistIncident("ResolvedBefore", "RESOLVED", "MAJOR", dayStart.minusDays(3), app);
        old.setResolvedAt(dayStart.minusDays(2));
        em.persistAndFlush(old);

        assertThat(repository.findPublicIncidentsAffectingDate(app.getId(), dayStart, dayEnd))
                .extracting(StatusIncident::getTitle)
                .containsExactlyInAnyOrder("StartedToday", "OngoingFromBefore");
    }
}
