package org.automatize.status.repositories;

import org.automatize.status.models.LogMetric;
import org.automatize.status.models.Tenant;
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
 * {@code @DataJpaTest} coverage for {@link LogMetricRepository}'s JPQL/derived finders
 * against H2 (PostgreSQL compatibility mode). Focus is time-window ({@code findSince})
 * queries, bucket-type filtering, optional tenant scoping, JPQL SUM aggregation and the
 * exact upsert lookup. The repository declares no {@code nativeQuery = true} methods, so
 * nothing is skipped here.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class LogMetricRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private LogMetricRepository repository;

    private static final ZonedDateTime BASE = ZonedDateTime.parse("2026-01-01T00:00:00Z");

    private Tenant persistTenant(String name) {
        Tenant t = new Tenant();
        t.setName(name);
        t.setCreatedBy("test");
        t.setLastModifiedBy("test");
        return em.persistAndFlush(t);
    }

    private LogMetric persistMetric(Tenant tenant, String service, String level, ZonedDateTime bucket,
                                    String bucketType, long count) {
        LogMetric m = new LogMetric();
        m.setTenant(tenant);
        m.setService(service);
        m.setLevel(level);
        m.setBucket(bucket);
        m.setBucketType(bucketType);
        m.setCount(count);
        return em.persistAndFlush(m);
    }

    @Test
    void findSince_returnsBucketsAtOrAfterBoundaryOrderedAscending() {
        Tenant t = persistTenant("T1");
        persistMetric(t, "svc", "INFO", BASE.minusMinutes(1), "MINUTE", 1);
        persistMetric(t, "svc", "INFO", BASE.plusMinutes(2), "MINUTE", 1);
        persistMetric(t, "svc", "INFO", BASE, "MINUTE", 1);

        List<LogMetric> result = repository.findSince(BASE);

        assertThat(result).extracting(LogMetric::getBucket).containsExactly(BASE, BASE.plusMinutes(2));
    }

    @Test
    void findSinceByType_filtersByBucketType() {
        Tenant t = persistTenant("T1");
        persistMetric(t, "svc", "INFO", BASE.plusMinutes(1), "MINUTE", 1);
        persistMetric(t, "svc", "INFO", BASE.plusMinutes(2), "HOUR", 1);
        persistMetric(t, "svc", "INFO", BASE.minusMinutes(5), "HOUR", 1);

        List<LogMetric> result = repository.findSinceByType(BASE, "HOUR");

        assertThat(result).extracting(LogMetric::getBucket).containsExactly(BASE.plusMinutes(2));
    }

    @Test
    void findByTenantSince_withTenant_scopesToTenant() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        persistMetric(t1, "svc", "INFO", BASE.plusMinutes(1), "MINUTE", 1);
        persistMetric(t2, "svc", "INFO", BASE.plusMinutes(2), "MINUTE", 1);

        List<LogMetric> result = repository.findByTenantSince(t1.getId(), BASE);

        assertThat(result).extracting(LogMetric::getBucket).containsExactly(BASE.plusMinutes(1));
    }

    @Test
    void findByTenantSince_withNullTenant_returnsAllTenants() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        persistMetric(t1, "svc", "INFO", BASE.plusMinutes(1), "MINUTE", 1);
        persistMetric(t2, "svc", "INFO", BASE.plusMinutes(2), "MINUTE", 1);

        List<LogMetric> result = repository.findByTenantSince(null, BASE);

        assertThat(result).extracting(LogMetric::getBucket).containsExactly(BASE.plusMinutes(1), BASE.plusMinutes(2));
    }

    @Test
    void sumCountSince_withoutFilters_sumsAllBucketsInWindow() {
        Tenant t = persistTenant("T1");
        persistMetric(t, "api", "INFO", BASE.plusMinutes(1), "MINUTE", 5);
        persistMetric(t, "web", "ERROR", BASE.plusMinutes(2), "MINUTE", 7);
        persistMetric(t, "api", "INFO", BASE.minusMinutes(5), "MINUTE", 99);

        assertThat(repository.sumCountSince(null, null, BASE)).isEqualTo(12L);
    }

    @Test
    void sumCountSince_withServiceAndLevelFilters_sumsMatchingBuckets() {
        Tenant t = persistTenant("T1");
        persistMetric(t, "api", "ERROR", BASE.plusMinutes(1), "MINUTE", 5);
        persistMetric(t, "api", "ERROR", BASE.plusMinutes(2), "MINUTE", 3);
        persistMetric(t, "api", "INFO", BASE.plusMinutes(3), "MINUTE", 10);
        persistMetric(t, "web", "ERROR", BASE.plusMinutes(4), "MINUTE", 20);

        assertThat(repository.sumCountSince("api", "ERROR", BASE)).isEqualTo(8L);
    }

    @Test
    void sumCountSince_noMatchingBuckets_returnsNull() {
        Tenant t = persistTenant("T1");
        persistMetric(t, "api", "INFO", BASE.minusMinutes(5), "MINUTE", 5);

        assertThat(repository.sumCountSince(null, null, BASE)).isNull();
    }

    @Test
    void findByTenantIdAndServiceAndLevelAndBucketAndBucketType_matchesExactCombination() {
        Tenant t = persistTenant("T1");
        persistMetric(t, "api", "ERROR", BASE, "MINUTE", 5);
        persistMetric(t, "api", "INFO", BASE, "MINUTE", 5);

        assertThat(repository.findByTenantIdAndServiceAndLevelAndBucketAndBucketType(
                t.getId(), "api", "ERROR", BASE, "MINUTE")).isPresent().get()
                .extracting(LogMetric::getLevel).isEqualTo("ERROR");
        assertThat(repository.findByTenantIdAndServiceAndLevelAndBucketAndBucketType(
                t.getId(), "api", "WARN", BASE, "MINUTE")).isEmpty();
    }
}
