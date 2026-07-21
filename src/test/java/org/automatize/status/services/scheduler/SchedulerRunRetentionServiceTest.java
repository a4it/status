package org.automatize.status.services.scheduler;

import org.automatize.status.models.SchedulerJob;
import org.automatize.status.repositories.SchedulerJobRepository;
import org.automatize.status.repositories.SchedulerJobRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SchedulerRunRetentionService}, the scheduled cleanup task
 * that deletes job runs older than a configured retention window, per job, while
 * tolerating individual per-job failures.
 *
 * <p>Repositories are Mockito mocks; the {@code retentionDays} property is set via
 * reflection per test.</p>
 */
@ExtendWith(MockitoExtension.class)
class SchedulerRunRetentionServiceTest {

    private static final String RETENTION_DAYS_FIELD = "retentionDays";

    @Mock private SchedulerJobRepository jobRepository;
    @Mock private SchedulerJobRunRepository runRepository;

    @InjectMocks private SchedulerRunRetentionService service;

    /**
     * Builds a minimal job carrying only the given id.
     *
     * @param id the job id to assign
     * @return a new {@link SchedulerJob}
     */
    private SchedulerJob jobWithId(UUID id) {
        SchedulerJob job = new SchedulerJob();
        job.setId(id);
        return job;
    }

    /**
     * Verifies each job's old runs are deleted using a cutoff of now minus the retention window.
     * Expected outcome: a delete per job with the captured cutoff within the expected time bounds.
     */
    @Test
    void cleanOldRuns_deletesRunsForEachJobWithCorrectCutoff() {
        ReflectionTestUtils.setField(service, RETENTION_DAYS_FIELD, 30);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(jobRepository.findAll()).thenReturn(List.of(jobWithId(id1), jobWithId(id2)));

        ZonedDateTime before = ZonedDateTime.now().minusDays(30);
        service.cleanOldRuns();
        ZonedDateTime after = ZonedDateTime.now().minusDays(30);

        ArgumentCaptor<ZonedDateTime> cutoffCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        verify(runRepository).deleteByJobIdAndStartedAtBefore(eq(id1), cutoffCaptor.capture());
        verify(runRepository).deleteByJobIdAndStartedAtBefore(eq(id2), any(ZonedDateTime.class));

        ZonedDateTime cutoff = cutoffCaptor.getValue();
        assertThat(cutoff).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    /**
     * Verifies a custom retention window is honoured when computing the cutoff.
     * Expected outcome: the captured cutoff falls within now minus the configured days.
     */
    @Test
    void cleanOldRuns_customRetentionDays_appliesConfiguredCutoff() {
        ReflectionTestUtils.setField(service, RETENTION_DAYS_FIELD, 7);
        UUID id = UUID.randomUUID();
        when(jobRepository.findAll()).thenReturn(List.of(jobWithId(id)));

        ZonedDateTime before = ZonedDateTime.now().minusDays(7);
        service.cleanOldRuns();
        ZonedDateTime after = ZonedDateTime.now().minusDays(7);

        ArgumentCaptor<ZonedDateTime> cutoffCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        verify(runRepository).deleteByJobIdAndStartedAtBefore(eq(id), cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    /**
     * Verifies a failure deleting one job's runs does not stop processing of the others.
     * Expected outcome: the remaining job is still processed and both deletes are attempted.
     */
    @Test
    void cleanOldRuns_oneJobFails_stillProcessesRemainingJobs() {
        ReflectionTestUtils.setField(service, RETENTION_DAYS_FIELD, 30);
        UUID failing = UUID.randomUUID();
        UUID ok = UUID.randomUUID();
        when(jobRepository.findAll()).thenReturn(List.of(jobWithId(failing), jobWithId(ok)));
        doThrow(new RuntimeException("db error"))
                .when(runRepository).deleteByJobIdAndStartedAtBefore(eq(failing), any(ZonedDateTime.class));

        service.cleanOldRuns();

        verify(runRepository).deleteByJobIdAndStartedAtBefore(eq(ok), any(ZonedDateTime.class));
        verify(runRepository, times(2)).deleteByJobIdAndStartedAtBefore(any(UUID.class), any(ZonedDateTime.class));
    }

    /**
     * Verifies that with no jobs present no delete is attempted.
     * Expected outcome: the delete method is never invoked.
     */
    @Test
    void cleanOldRuns_noJobs_doesNothing() {
        ReflectionTestUtils.setField(service, RETENTION_DAYS_FIELD, 30);
        when(jobRepository.findAll()).thenReturn(List.of());

        service.cleanOldRuns();

        verify(runRepository, times(0)).deleteByJobIdAndStartedAtBefore(any(), any());
    }
}
