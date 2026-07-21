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

@ExtendWith(MockitoExtension.class)
class SchedulerRunRetentionServiceTest {

    @Mock private SchedulerJobRepository jobRepository;
    @Mock private SchedulerJobRunRepository runRepository;

    @InjectMocks private SchedulerRunRetentionService service;

    private SchedulerJob jobWithId(UUID id) {
        SchedulerJob job = new SchedulerJob();
        job.setId(id);
        return job;
    }

    @Test
    void cleanOldRuns_deletesRunsForEachJobWithCorrectCutoff() {
        ReflectionTestUtils.setField(service, "retentionDays", 30);
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

    @Test
    void cleanOldRuns_customRetentionDays_appliesConfiguredCutoff() {
        ReflectionTestUtils.setField(service, "retentionDays", 7);
        UUID id = UUID.randomUUID();
        when(jobRepository.findAll()).thenReturn(List.of(jobWithId(id)));

        ZonedDateTime before = ZonedDateTime.now().minusDays(7);
        service.cleanOldRuns();
        ZonedDateTime after = ZonedDateTime.now().minusDays(7);

        ArgumentCaptor<ZonedDateTime> cutoffCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        verify(runRepository).deleteByJobIdAndStartedAtBefore(eq(id), cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void cleanOldRuns_oneJobFails_stillProcessesRemainingJobs() {
        ReflectionTestUtils.setField(service, "retentionDays", 30);
        UUID failing = UUID.randomUUID();
        UUID ok = UUID.randomUUID();
        when(jobRepository.findAll()).thenReturn(List.of(jobWithId(failing), jobWithId(ok)));
        doThrow(new RuntimeException("db error"))
                .when(runRepository).deleteByJobIdAndStartedAtBefore(eq(failing), any(ZonedDateTime.class));

        service.cleanOldRuns();

        verify(runRepository).deleteByJobIdAndStartedAtBefore(eq(ok), any(ZonedDateTime.class));
        verify(runRepository, times(2)).deleteByJobIdAndStartedAtBefore(any(UUID.class), any(ZonedDateTime.class));
    }

    @Test
    void cleanOldRuns_noJobs_doesNothing() {
        ReflectionTestUtils.setField(service, "retentionDays", 30);
        when(jobRepository.findAll()).thenReturn(List.of());

        service.cleanOldRuns();

        verify(runRepository, times(0)).deleteByJobIdAndStartedAtBefore(any(), any());
    }
}
