package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.StatusUptimeHistory;
import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * {@code @DataJpaTest} coverage for {@link StatusUptimeHistoryRepository}'s custom
 * JPQL queries against H2 (PostgreSQL compatibility mode). Focuses on app-level
 * (component IS NULL) versus component-level record separation, date-range
 * history, single-date lookups, average-uptime and incident-sum aggregates, and
 * derived ordered finders.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class StatusUptimeHistoryRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private StatusUptimeHistoryRepository repository;

    private Tenant tenant;
    private Organization organization;
    private StatusApp app;
    private StatusComponent component;

    private final LocalDate day1 = LocalDate.of(2026, 1, 1);
    private final LocalDate day2 = LocalDate.of(2026, 1, 2);
    private final LocalDate day3 = LocalDate.of(2026, 1, 3);

    @BeforeEach
    void setUp() {
        tenant = persistTenant("Tenant A");
        organization = persistOrganization("Org A", tenant);
        app = persistApp("App A", "app-a", tenant, organization);
        component = persistComponent("API", app);
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

    /** Persists an app-level record (component == null). */
    private StatusUptimeHistory persistAppRecord(LocalDate date, String uptime, int incidents) {
        StatusUptimeHistory h = new StatusUptimeHistory();
        h.setApp(app);
        h.setRecordDate(date);
        h.setUptimePercentage(new BigDecimal(uptime));
        h.setIncidentCount(incidents);
        return em.persistAndFlush(h);
    }

    /** Persists a component-level record. */
    private StatusUptimeHistory persistComponentRecord(LocalDate date, String uptime, int incidents) {
        StatusUptimeHistory h = new StatusUptimeHistory();
        h.setApp(app);
        h.setComponent(component);
        h.setRecordDate(date);
        h.setUptimePercentage(new BigDecimal(uptime));
        h.setIncidentCount(incidents);
        return em.persistAndFlush(h);
    }

    @Test
    void findAppUptimeHistory_returnsAppLevelRecordsInRangeOrdered() {
        persistAppRecord(day3, "99.000", 0);
        persistAppRecord(day1, "100.000", 0);
        persistAppRecord(day2, "98.000", 0);
        // Component-level record must be excluded.
        persistComponentRecord(day1, "50.000", 5);

        assertThat(repository.findAppUptimeHistory(app.getId(), day1, day3))
                .extracting(StatusUptimeHistory::getRecordDate)
                .containsExactly(day1, day2, day3);
    }

    @Test
    void findAppUptimeHistory_excludesRecordsOutsideRange() {
        persistAppRecord(day1, "100.000", 0);
        persistAppRecord(day3, "100.000", 0);

        assertThat(repository.findAppUptimeHistory(app.getId(), day1, day2))
                .extracting(StatusUptimeHistory::getRecordDate)
                .containsExactly(day1);
    }

    @Test
    void findComponentUptimeHistory_returnsComponentRecordsInRangeOrdered() {
        persistComponentRecord(day2, "97.000", 1);
        persistComponentRecord(day1, "99.000", 0);
        // App-level record must be excluded.
        persistAppRecord(day1, "100.000", 0);

        assertThat(repository.findComponentUptimeHistory(component.getId(), day1, day3))
                .extracting(StatusUptimeHistory::getRecordDate)
                .containsExactly(day1, day2);
    }

    @Test
    void findAppUptimeByDate_returnsAppLevelRecordForDate() {
        persistAppRecord(day1, "100.000", 0);
        persistComponentRecord(day1, "50.000", 5);

        assertThat(repository.findAppUptimeByDate(app.getId(), day1))
                .isPresent()
                .get().extracting(StatusUptimeHistory::getComponent).isNull();
        assertThat(repository.findAppUptimeByDate(app.getId(), day2)).isEmpty();
    }

    @Test
    void findComponentUptimeByDate_returnsComponentRecordForDate() {
        persistComponentRecord(day1, "99.000", 0);

        assertThat(repository.findComponentUptimeByDate(component.getId(), day1))
                .isPresent()
                .get().extracting(h -> h.getComponent().getId()).isEqualTo(component.getId());
        assertThat(repository.findComponentUptimeByDate(component.getId(), day2)).isEmpty();
    }

    @Test
    void calculateAverageAppUptime_averagesAppLevelRecords() {
        persistAppRecord(day1, "100.000", 0);
        persistAppRecord(day2, "98.000", 0);
        // Component-level record must not affect the app-level average.
        persistComponentRecord(day1, "10.000", 0);

        assertThat(repository.calculateAverageAppUptime(app.getId(), day1, day3))
                .isCloseTo(99.0, within(0.001));
    }

    @Test
    void calculateAverageAppUptime_returnsNullWhenNoRecords() {
        assertThat(repository.calculateAverageAppUptime(app.getId(), day1, day3)).isNull();
    }

    @Test
    void calculateAverageComponentUptime_averagesComponentRecords() {
        persistComponentRecord(day1, "96.000", 0);
        persistComponentRecord(day2, "98.000", 0);

        assertThat(repository.calculateAverageComponentUptime(component.getId(), day1, day3))
                .isCloseTo(97.0, within(0.001));
    }

    @Test
    void countAppIncidents_sumsAppLevelIncidentCounts() {
        persistAppRecord(day1, "100.000", 2);
        persistAppRecord(day2, "100.000", 3);
        // Component-level incidents must be excluded.
        persistComponentRecord(day1, "100.000", 10);

        assertThat(repository.countAppIncidents(app.getId(), day1, day3)).isEqualTo(5L);
    }

    @Test
    void countComponentIncidents_sumsComponentIncidentCounts() {
        persistComponentRecord(day1, "100.000", 4);
        persistComponentRecord(day2, "100.000", 1);

        assertThat(repository.countComponentIncidents(component.getId(), day1, day3)).isEqualTo(5L);
    }

    @Test
    void findByAppIdAndComponentIsNullOrderByRecordDateAsc_returnsAppLevelOrdered() {
        persistAppRecord(day2, "99.000", 0);
        persistAppRecord(day1, "100.000", 0);
        persistComponentRecord(day1, "50.000", 0);

        assertThat(repository.findByAppIdAndComponentIsNullOrderByRecordDateAsc(app.getId()))
                .extracting(StatusUptimeHistory::getRecordDate)
                .containsExactly(day1, day2);
    }

    @Test
    void findByComponentIdOrderByRecordDateAsc_returnsComponentRecordsOrdered() {
        persistComponentRecord(day3, "97.000", 0);
        persistComponentRecord(day1, "99.000", 0);
        persistComponentRecord(day2, "98.000", 0);

        assertThat(repository.findByComponentIdOrderByRecordDateAsc(component.getId()))
                .extracting(StatusUptimeHistory::getRecordDate)
                .containsExactly(day1, day2, day3);
    }
}
