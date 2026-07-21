package org.automatize.status.services;

import org.automatize.status.models.LogMetric;
import org.automatize.status.repositories.LogMetricRepository;
import org.automatize.status.repositories.LogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LogMetricService} — query delegation and the
 * aggregate-recent-logs upsert branches (create new bucket vs. increment existing).
 *
 * <p>Testing approach: the service is tested in isolation with Mockito. Its
 * {@link LogMetricRepository} and {@link LogRepository} collaborators are
 * {@code @Mock}s injected via {@code @InjectMocks}; repository queries are stubbed and
 * {@link ArgumentCaptor}s inspect the {@link LogMetric} entities passed to {@code save}.</p>
 */
@ExtendWith(MockitoExtension.class)
class LogMetricServiceTest {

    private static final String LEVEL_ERROR = "ERROR";
    private static final String SERVICE_BILLING = "billing";

    @Mock
    private LogMetricRepository logMetricRepository;

    @Mock
    private LogRepository logRepository;

    @InjectMocks
    private LogMetricService service;

    // ── simple query delegation ───────────────────────────────────────────────

    /**
     * Verifies that {@link LogMetricService#findSince} delegates to the repository and
     * returns its list of metrics.
     */
    @Test
    void findSince_delegatesToRepository() {
        ZonedDateTime since = ZonedDateTime.now().minusHours(1);
        LogMetric m = new LogMetric();
        when(logMetricRepository.findSince(since)).thenReturn(List.of(m));

        assertThat(service.findSince(since)).containsExactly(m);
    }

    /**
     * Verifies that {@link LogMetricService#findByTenantSince} delegates to the
     * tenant-scoped repository query and returns its result.
     */
    @Test
    void findByTenantSince_delegatesToRepository() {
        UUID tenantId = UUID.randomUUID();
        ZonedDateTime since = ZonedDateTime.now().minusHours(1);
        when(logMetricRepository.findByTenantSince(tenantId, since)).thenReturn(List.of());

        assertThat(service.findByTenantSince(tenantId, since)).isEmpty();
        verify(logMetricRepository).findByTenantSince(tenantId, since);
    }

    /**
     * Verifies that {@link LogMetricService#sumCountSince} returns the repository's sum
     * when it is non-null.
     */
    @Test
    void sumCountSince_nonNullResult_returnsValue() {
        ZonedDateTime since = ZonedDateTime.now().minusMinutes(5);
        when(logMetricRepository.sumCountSince("svc", LEVEL_ERROR, since)).thenReturn(17L);

        assertThat(service.sumCountSince("svc", LEVEL_ERROR, since)).isEqualTo(17L);
    }

    /**
     * Verifies that {@link LogMetricService#sumCountSince} coalesces a null repository
     * result to zero.
     */
    @Test
    void sumCountSince_nullResult_returnsZero() {
        ZonedDateTime since = ZonedDateTime.now().minusMinutes(5);
        when(logMetricRepository.sumCountSince("svc", LEVEL_ERROR, since)).thenReturn(null);

        assertThat(service.sumCountSince("svc", LEVEL_ERROR, since)).isZero();
    }

    // ── aggregateRecentLogs (upsert branches) ─────────────────────────────────

    /**
     * Verifies the create branch of aggregation: when no existing metric matches the
     * bucket, a new MINUTE-bucket {@link LogMetric} is saved with the aggregated service,
     * level, count, and resolved tenant.
     */
    @Test
    void aggregateRecentLogs_newBucket_createsMetricWithTenant() {
        UUID tenantId = UUID.randomUUID();
        Object[] row = new Object[]{tenantId, SERVICE_BILLING, LEVEL_ERROR, 5L};
        when(logRepository.aggregateByServiceLevel(any(), any())).thenReturn(List.<Object[]>of(row));
        when(logMetricRepository.findByTenantIdAndServiceAndLevelAndBucketAndBucketType(
                eq(tenantId), eq(SERVICE_BILLING), eq(LEVEL_ERROR), any(), eq("MINUTE")))
                .thenReturn(Optional.empty());

        service.aggregateRecentLogs();

        ArgumentCaptor<LogMetric> captor = ArgumentCaptor.forClass(LogMetric.class);
        verify(logMetricRepository).save(captor.capture());
        LogMetric saved = captor.getValue();
        assertThat(saved.getService()).isEqualTo(SERVICE_BILLING);
        assertThat(saved.getLevel()).isEqualTo(LEVEL_ERROR);
        assertThat(saved.getBucketType()).isEqualTo("MINUTE");
        assertThat(saved.getCount()).isEqualTo(5L);
        assertThat(saved.getTenant()).isNotNull();
        assertThat(saved.getTenant().getId()).isEqualTo(tenantId);
    }

    /**
     * Verifies that when the aggregated row has a null tenant id, a new metric is saved
     * with a null tenant and the aggregated count.
     */
    @Test
    void aggregateRecentLogs_nullTenant_createsMetricWithoutTenant() {
        Object[] row = new Object[]{null, SERVICE_BILLING, "INFO", 3L};
        when(logRepository.aggregateByServiceLevel(any(), any())).thenReturn(List.<Object[]>of(row));
        when(logMetricRepository.findByTenantIdAndServiceAndLevelAndBucketAndBucketType(
                any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        service.aggregateRecentLogs();

        ArgumentCaptor<LogMetric> captor = ArgumentCaptor.forClass(LogMetric.class);
        verify(logMetricRepository).save(captor.capture());
        assertThat(captor.getValue().getTenant()).isNull();
        assertThat(captor.getValue().getCount()).isEqualTo(3L);
    }

    /**
     * Verifies the increment branch of aggregation: when a metric already exists for the
     * bucket, its count is increased by the aggregated amount (10 + 4 = 14) and the same
     * entity is re-saved.
     */
    @Test
    void aggregateRecentLogs_existingBucket_incrementsCount() {
        Object[] row = new Object[]{null, SERVICE_BILLING, "INFO", 4L};
        when(logRepository.aggregateByServiceLevel(any(), any())).thenReturn(List.<Object[]>of(row));

        LogMetric existing = new LogMetric();
        existing.setService(SERVICE_BILLING);
        existing.setLevel("INFO");
        existing.setCount(10L);
        when(logMetricRepository.findByTenantIdAndServiceAndLevelAndBucketAndBucketType(
                any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(existing));

        service.aggregateRecentLogs();

        ArgumentCaptor<LogMetric> captor = ArgumentCaptor.forClass(LogMetric.class);
        verify(logMetricRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(captor.getValue().getCount()).isEqualTo(14L);
    }

    /**
     * Verifies that when the aggregation query returns no rows, no metric is saved.
     */
    @Test
    void aggregateRecentLogs_noRows_savesNothing() {
        when(logRepository.aggregateByServiceLevel(any(), any())).thenReturn(List.of());

        service.aggregateRecentLogs();

        verify(logMetricRepository, org.mockito.Mockito.never()).save(any());
    }
}
