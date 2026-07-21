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
 * Unit tests for {@link LogMetricService}.
 */
@ExtendWith(MockitoExtension.class)
class LogMetricServiceTest {

    @Mock
    private LogMetricRepository logMetricRepository;

    @Mock
    private LogRepository logRepository;

    @InjectMocks
    private LogMetricService service;

    // ── simple query delegation ───────────────────────────────────────────────

    @Test
    void findSince_delegatesToRepository() {
        ZonedDateTime since = ZonedDateTime.now().minusHours(1);
        LogMetric m = new LogMetric();
        when(logMetricRepository.findSince(since)).thenReturn(List.of(m));

        assertThat(service.findSince(since)).containsExactly(m);
    }

    @Test
    void findByTenantSince_delegatesToRepository() {
        UUID tenantId = UUID.randomUUID();
        ZonedDateTime since = ZonedDateTime.now().minusHours(1);
        when(logMetricRepository.findByTenantSince(tenantId, since)).thenReturn(List.of());

        assertThat(service.findByTenantSince(tenantId, since)).isEmpty();
        verify(logMetricRepository).findByTenantSince(tenantId, since);
    }

    @Test
    void sumCountSince_nonNullResult_returnsValue() {
        ZonedDateTime since = ZonedDateTime.now().minusMinutes(5);
        when(logMetricRepository.sumCountSince("svc", "ERROR", since)).thenReturn(17L);

        assertThat(service.sumCountSince("svc", "ERROR", since)).isEqualTo(17L);
    }

    @Test
    void sumCountSince_nullResult_returnsZero() {
        ZonedDateTime since = ZonedDateTime.now().minusMinutes(5);
        when(logMetricRepository.sumCountSince("svc", "ERROR", since)).thenReturn(null);

        assertThat(service.sumCountSince("svc", "ERROR", since)).isZero();
    }

    // ── aggregateRecentLogs (upsert branches) ─────────────────────────────────

    @Test
    void aggregateRecentLogs_newBucket_createsMetricWithTenant() {
        UUID tenantId = UUID.randomUUID();
        Object[] row = new Object[]{tenantId, "billing", "ERROR", 5L};
        when(logRepository.aggregateByServiceLevel(any(), any())).thenReturn(List.of(row));
        when(logMetricRepository.findByTenantIdAndServiceAndLevelAndBucketAndBucketType(
                eq(tenantId), eq("billing"), eq("ERROR"), any(), eq("MINUTE")))
                .thenReturn(Optional.empty());

        service.aggregateRecentLogs();

        ArgumentCaptor<LogMetric> captor = ArgumentCaptor.forClass(LogMetric.class);
        verify(logMetricRepository).save(captor.capture());
        LogMetric saved = captor.getValue();
        assertThat(saved.getService()).isEqualTo("billing");
        assertThat(saved.getLevel()).isEqualTo("ERROR");
        assertThat(saved.getBucketType()).isEqualTo("MINUTE");
        assertThat(saved.getCount()).isEqualTo(5L);
        assertThat(saved.getTenant()).isNotNull();
        assertThat(saved.getTenant().getId()).isEqualTo(tenantId);
    }

    @Test
    void aggregateRecentLogs_nullTenant_createsMetricWithoutTenant() {
        Object[] row = new Object[]{null, "billing", "INFO", 3L};
        when(logRepository.aggregateByServiceLevel(any(), any())).thenReturn(List.of(row));
        when(logMetricRepository.findByTenantIdAndServiceAndLevelAndBucketAndBucketType(
                any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        service.aggregateRecentLogs();

        ArgumentCaptor<LogMetric> captor = ArgumentCaptor.forClass(LogMetric.class);
        verify(logMetricRepository).save(captor.capture());
        assertThat(captor.getValue().getTenant()).isNull();
        assertThat(captor.getValue().getCount()).isEqualTo(3L);
    }

    @Test
    void aggregateRecentLogs_existingBucket_incrementsCount() {
        Object[] row = new Object[]{null, "billing", "INFO", 4L};
        when(logRepository.aggregateByServiceLevel(any(), any())).thenReturn(List.of(row));

        LogMetric existing = new LogMetric();
        existing.setService("billing");
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

    @Test
    void aggregateRecentLogs_noRows_savesNothing() {
        when(logRepository.aggregateByServiceLevel(any(), any())).thenReturn(List.of());

        service.aggregateRecentLogs();

        verify(logMetricRepository, org.mockito.Mockito.never()).save(any());
    }
}
