package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusMaintenance;
import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} coverage for {@link StatusMaintenanceRepository}'s custom
 * JPQL / derived queries against H2 (PostgreSQL compatibility mode). Focuses on
 * app/tenant/organization scoping, status/visibility filters, start/end date
 * ranges, active and upcoming windows, ordering, search and counts.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class StatusMaintenanceRepositoryTest extends AbstractAppScopedRepositoryTest {

    private static final String SCHEDULED = "SCHEDULED";
    private static final String APP_B = "App B";
    private static final String APP_B_SLUG = "app-b";
    private static final String SCHEDULED_TITLE = "Scheduled";
    private static final String COMPLETED = "COMPLETED";
    private static final String PRIVATE = "Private";
    private static final String PUBLIC = "Public";
    private static final String IN_RANGE = "InRange";
    private static final String ORG_B = "Org B";
    private static final String DB_UPGRADE = "DB upgrade";

    @Autowired
    private StatusMaintenanceRepository repository;

    private final ZonedDateTime now = ZonedDateTime.now();

    private StatusMaintenance persistMaintenance(String title, String status,
                                                 ZonedDateTime startsAt, ZonedDateTime endsAt, StatusApp app) {
        StatusMaintenance m = new StatusMaintenance();
        m.setTitle(title);
        m.setStatus(status);
        m.setStartsAt(startsAt);
        m.setEndsAt(endsAt);
        m.setApp(app);
        m.setCreatedBy("test");
        m.setLastModifiedBy("test");
        return em.persistAndFlush(m);
    }

    @Test
    void findByAppId_returnsMaintenanceOfApp() {
        persistMaintenance("Patch", SCHEDULED, now, now.plusHours(1), app);
        StatusApp other = persistApp(APP_B, APP_B_SLUG, tenant, organization);
        persistMaintenance("Other", SCHEDULED, now, now.plusHours(1), other);

        assertThat(repository.findByAppId(app.getId()))
                .extracting(StatusMaintenance::getTitle).containsExactly("Patch");
    }

    @Test
    void findByStatus_filtersByStatus() {
        persistMaintenance(SCHEDULED_TITLE, SCHEDULED, now, now.plusHours(1), app);
        persistMaintenance("Done", COMPLETED, now, now.plusHours(1), app);

        assertThat(repository.findByStatus(COMPLETED))
                .extracting(StatusMaintenance::getTitle).containsExactly("Done");
    }

    @Test
    void findByIsPublic_filtersByVisibility() {
        StatusMaintenance priv = persistMaintenance(PRIVATE, SCHEDULED, now, now.plusHours(1), app);
        priv.setIsPublic(false);
        em.persistAndFlush(priv);
        persistMaintenance(PUBLIC, SCHEDULED, now, now.plusHours(1), app);

        assertThat(repository.findByIsPublic(false))
                .extracting(StatusMaintenance::getTitle).containsExactly(PRIVATE);
    }

    @Test
    void findByAppIdAndStatus_combinesAppAndStatus() {
        persistMaintenance(SCHEDULED_TITLE, SCHEDULED, now, now.plusHours(1), app);
        persistMaintenance("Done", COMPLETED, now, now.plusHours(1), app);

        assertThat(repository.findByAppIdAndStatus(app.getId(), SCHEDULED))
                .extracting(StatusMaintenance::getTitle).containsExactly(SCHEDULED_TITLE);
    }

    @Test
    void findByAppIdAndIsPublic_combinesAppAndVisibility() {
        persistMaintenance(PUBLIC, SCHEDULED, now, now.plusHours(1), app);
        StatusMaintenance priv = persistMaintenance(PRIVATE, SCHEDULED, now, now.plusHours(1), app);
        priv.setIsPublic(false);
        em.persistAndFlush(priv);

        assertThat(repository.findByAppIdAndIsPublic(app.getId(), true))
                .extracting(StatusMaintenance::getTitle).containsExactly(PUBLIC);
    }

    @Test
    void findByStartsAtBetween_returnsWindowsStartingInRange() {
        persistMaintenance(IN_RANGE, SCHEDULED, now.plusHours(1), now.plusHours(2), app);
        persistMaintenance("OutOfRange", SCHEDULED, now.plusDays(10), now.plusDays(10).plusHours(1), app);

        assertThat(repository.findByStartsAtBetween(now, now.plusHours(3)))
                .extracting(StatusMaintenance::getTitle).containsExactly(IN_RANGE);
    }

    @Test
    void findByEndsAtBetween_returnsWindowsEndingInRange() {
        persistMaintenance(IN_RANGE, SCHEDULED, now, now.plusHours(2), app);
        persistMaintenance("OutOfRange", SCHEDULED, now, now.plusDays(10), app);

        assertThat(repository.findByEndsAtBetween(now.plusHours(1), now.plusHours(3)))
                .extracting(StatusMaintenance::getTitle).containsExactly(IN_RANGE);
    }

    @Test
    void findByTenantId_scopesByAppTenant() {
        persistMaintenance("Mine", SCHEDULED, now, now.plusHours(1), app);
        Tenant other = persistTenant("Tenant B");
        Organization otherOrg = persistOrganization(ORG_B, other);
        StatusApp otherApp = persistApp(APP_B, APP_B_SLUG, other, otherOrg);
        persistMaintenance("Theirs", SCHEDULED, now, now.plusHours(1), otherApp);

        assertThat(repository.findByTenantId(tenant.getId()))
                .extracting(StatusMaintenance::getTitle).containsExactly("Mine");
    }

    @Test
    void findByOrganizationId_scopesByAppOrganization() {
        persistMaintenance("Mine", SCHEDULED, now, now.plusHours(1), app);
        Organization otherOrg = persistOrganization(ORG_B, tenant);
        StatusApp otherApp = persistApp(APP_B, APP_B_SLUG, tenant, otherOrg);
        persistMaintenance("Theirs", SCHEDULED, now, now.plusHours(1), otherApp);

        assertThat(repository.findByOrganizationId(organization.getId()))
                .extracting(StatusMaintenance::getTitle).containsExactly("Mine");
    }

    @Test
    void findByAppIdOrderByStartsAtDesc_ordersMostRecentFirst() {
        persistMaintenance("Old", SCHEDULED, now.minusDays(2), now.minusDays(2).plusHours(1), app);
        persistMaintenance("New", SCHEDULED, now, now.plusHours(1), app);
        persistMaintenance("Mid", SCHEDULED, now.minusDays(1), now.minusDays(1).plusHours(1), app);

        assertThat(repository.findByAppIdOrderByStartsAtDesc(app.getId()))
                .extracting(StatusMaintenance::getTitle).containsExactly("New", "Mid", "Old");
    }

    @Test
    void findActiveMaintenance_returnsWindowsSpanningNow() {
        persistMaintenance("Active", "IN_PROGRESS", now.minusHours(1), now.plusHours(1), app);
        persistMaintenance("Future", SCHEDULED, now.plusHours(2), now.plusHours(3), app);
        persistMaintenance("Past", COMPLETED, now.minusDays(1), now.minusHours(2), app);

        assertThat(repository.findActiveMaintenance(now))
                .extracting(StatusMaintenance::getTitle).containsExactly("Active");
    }

    @Test
    void findUpcomingMaintenanceByAppId_returnsThoseStartingOnOrAfterDate() {
        persistMaintenance("Upcoming", SCHEDULED, now.plusDays(1), now.plusDays(1).plusHours(1), app);
        persistMaintenance("Past", COMPLETED, now.minusDays(10), now.minusDays(10).plusHours(1), app);

        assertThat(repository.findUpcomingMaintenanceByAppId(app.getId(), now))
                .extracting(StatusMaintenance::getTitle).containsExactly("Upcoming");
    }

    @Test
    void search_matchesTitleOrDescription() {
        StatusMaintenance m = persistMaintenance(DB_UPGRADE, SCHEDULED, now, now.plusHours(1), app);
        m.setDescription("postgres major version");
        em.persistAndFlush(m);
        persistMaintenance("Network work", SCHEDULED, now, now.plusHours(1), app);

        assertThat(repository.search("upgrade"))
                .extracting(StatusMaintenance::getTitle).containsExactly(DB_UPGRADE);
        assertThat(repository.search("postgres"))
                .extracting(StatusMaintenance::getTitle).containsExactly(DB_UPGRADE);
    }

    @Test
    void countByAppId_countsMaintenance() {
        persistMaintenance("A", SCHEDULED, now, now.plusHours(1), app);
        persistMaintenance("B", COMPLETED, now, now.plusHours(1), app);

        assertThat(repository.countByAppId(app.getId())).isEqualTo(2L);
    }

    @Test
    void countByTenantId_countsScopedMaintenance() {
        persistMaintenance("A", SCHEDULED, now, now.plusHours(1), app);
        Tenant other = persistTenant("Tenant B");
        Organization otherOrg = persistOrganization(ORG_B, other);
        StatusApp otherApp = persistApp(APP_B, APP_B_SLUG, other, otherOrg);
        persistMaintenance("B", SCHEDULED, now, now.plusHours(1), otherApp);

        assertThat(repository.countByTenantId(tenant.getId())).isEqualTo(1L);
    }

    @Test
    void countActiveMaintenanceByAppId_countsScheduledOrInProgress() {
        persistMaintenance(SCHEDULED_TITLE, SCHEDULED, now, now.plusHours(1), app);
        persistMaintenance("InProgress", "IN_PROGRESS", now, now.plusHours(1), app);
        persistMaintenance("Completed", COMPLETED, now, now.plusHours(1), app);

        assertThat(repository.countActiveMaintenanceByAppId(app.getId())).isEqualTo(2L);
    }

    @Test
    void findUpcomingMaintenanceByAppIdIn_returnsPublicUpcomingOrdered() {
        persistMaintenance("Later", SCHEDULED, now.plusDays(2), now.plusDays(2).plusHours(1), app);
        persistMaintenance("Sooner", SCHEDULED, now.plusDays(1), now.plusDays(1).plusHours(1), app);
        StatusMaintenance priv = persistMaintenance("PrivateUpcoming", SCHEDULED,
                now.plusHours(5), now.plusHours(6), app);
        priv.setIsPublic(false);
        em.persistAndFlush(priv);
        persistMaintenance("Past", COMPLETED, now.minusDays(1), now.minusHours(1), app);

        List<StatusMaintenance> result =
                repository.findUpcomingMaintenanceByAppIdIn(List.of(app.getId()), now);

        assertThat(result).extracting(StatusMaintenance::getTitle).containsExactly("Sooner", "Later");
    }
}
