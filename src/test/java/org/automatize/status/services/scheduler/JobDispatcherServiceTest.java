package org.automatize.status.services.scheduler;

import org.automatize.status.models.SchedulerJob;
import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.SchedulerRestConfig;
import org.automatize.status.models.SchedulerSqlConfig;
import org.automatize.status.models.Tenant;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.JobStatus;
import org.automatize.status.models.scheduler.JobTriggerType;
import org.automatize.status.models.scheduler.JobType;
import org.automatize.status.repositories.SchedulerJobRepository;
import org.automatize.status.repositories.SchedulerJobRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobDispatcherServiceTest {

    @Mock private SchedulerJobRepository schedulerJobRepository;
    @Mock private SchedulerJobRunRepository schedulerJobRunRepository;
    @Mock private ProgramExecutorService programExecutorService;
    @Mock private SqlExecutorService sqlExecutorService;
    @Mock private RestExecutorService restExecutorService;
    @Mock private SoapExecutorService soapExecutorService;

    @InjectMocks private JobDispatcherService service;

    private final UUID jobId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    private SchedulerJob activeJob(JobType type) {
        SchedulerJob job = new SchedulerJob();
        job.setId(jobId);
        job.setTenant(new Tenant());
        job.setJobType(type);
        job.setEnabled(true);
        job.setStatus(JobStatus.ACTIVE);
        job.setAllowConcurrent(false);
        job.setConsecutiveFailures(0);
        return job;
    }

    // ---------------------------------------------------------------------
    // Routing by job type
    // ---------------------------------------------------------------------

    @Test
    void dispatch_restJob_routesToRestExecutor() {
        SchedulerJob job = activeJob(JobType.REST);
        job.setRestConfig(new SchedulerRestConfig());
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(schedulerJobRunRepository.findByJobIdAndStatus(jobId, JobRunStatus.RUNNING)).thenReturn(List.of());
        when(schedulerJobRunRepository.save(any(SchedulerJobRun.class))).thenAnswer(inv -> inv.getArgument(0));

        service.dispatch(jobId);

        verify(restExecutorService).execute(any(SchedulerRestConfig.class), any(SchedulerJobRun.class));
        verify(sqlExecutorService, never()).execute(any(), any());
        verify(schedulerJobRepository).save(job);
    }

    @Test
    void dispatch_sqlJob_routesToSqlExecutor() {
        SchedulerJob job = activeJob(JobType.SQL);
        job.setSqlConfig(new SchedulerSqlConfig());
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(schedulerJobRunRepository.findByJobIdAndStatus(jobId, JobRunStatus.RUNNING)).thenReturn(List.of());
        when(schedulerJobRunRepository.save(any(SchedulerJobRun.class))).thenAnswer(inv -> inv.getArgument(0));

        service.dispatch(jobId);

        verify(sqlExecutorService).execute(any(SchedulerSqlConfig.class), any(SchedulerJobRun.class));
        verify(restExecutorService, never()).execute(any(), any());
    }

    @Test
    void dispatch_successfulRun_recordsRunningTriggerTypeScheduled() {
        SchedulerJob job = activeJob(JobType.REST);
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(schedulerJobRunRepository.findByJobIdAndStatus(jobId, JobRunStatus.RUNNING)).thenReturn(List.of());
        when(schedulerJobRunRepository.save(any(SchedulerJobRun.class))).thenAnswer(inv -> inv.getArgument(0));
        // executor sets a successful status on the run
        doAnswer(inv -> {
            SchedulerJobRun r = inv.getArgument(1);
            r.setStatus(JobRunStatus.SUCCESS);
            return null;
        }).when(restExecutorService).execute(any(), any(SchedulerJobRun.class));

        service.dispatch(jobId);

        ArgumentCaptor<SchedulerJobRun> runCaptor = ArgumentCaptor.forClass(SchedulerJobRun.class);
        verify(schedulerJobRunRepository, org.mockito.Mockito.atLeastOnce()).save(runCaptor.capture());
        SchedulerJobRun run = runCaptor.getValue();
        assertThat(run.getTriggerType()).isEqualTo(JobTriggerType.SCHEDULED);
        assertThat(run.getStatus()).isEqualTo(JobRunStatus.SUCCESS);
        assertThat(run.getFinishedAt()).isNotNull();
        assertThat(job.getLastRunStatus()).isEqualTo(JobRunStatus.SUCCESS);
        assertThat(job.getConsecutiveFailures()).isZero();
    }

    // ---------------------------------------------------------------------
    // Enabled / status guards
    // ---------------------------------------------------------------------

    @Test
    void dispatch_disabledJob_skipsAndReturnsNull() {
        SchedulerJob job = activeJob(JobType.REST);
        job.setEnabled(false);
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        service.dispatch(jobId);

        verify(restExecutorService, never()).execute(any(), any());
        verify(schedulerJobRunRepository, never()).save(any());
    }

    @Test
    void dispatch_notActiveJob_skips() {
        SchedulerJob job = activeJob(JobType.REST);
        job.setStatus(JobStatus.PAUSED);
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        service.dispatch(jobId);

        verify(restExecutorService, never()).execute(any(), any());
        verify(schedulerJobRunRepository, never()).save(any());
    }

    @Test
    void dispatch_jobNotFound_returnsNullNoExecution() {
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.empty());

        service.dispatch(jobId);

        verify(schedulerJobRunRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // Concurrency guard
    // ---------------------------------------------------------------------

    @Test
    void dispatch_concurrentDisallowedAndAlreadyRunning_recordsSkipped() {
        SchedulerJob job = activeJob(JobType.REST);
        SchedulerJobRun alreadyRunning = new SchedulerJobRun();
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(schedulerJobRunRepository.findByJobIdAndStatus(jobId, JobRunStatus.RUNNING))
                .thenReturn(List.of(alreadyRunning));
        when(schedulerJobRunRepository.save(any(SchedulerJobRun.class))).thenAnswer(inv -> inv.getArgument(0));

        service.dispatch(jobId);

        ArgumentCaptor<SchedulerJobRun> runCaptor = ArgumentCaptor.forClass(SchedulerJobRun.class);
        verify(schedulerJobRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(JobRunStatus.SKIPPED);
        verify(restExecutorService, never()).execute(any(), any());
    }

    @Test
    void dispatch_concurrentAllowed_executesEvenWhenRunning() {
        SchedulerJob job = activeJob(JobType.REST);
        job.setAllowConcurrent(true);
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(schedulerJobRunRepository.save(any(SchedulerJobRun.class))).thenAnswer(inv -> inv.getArgument(0));

        service.dispatch(jobId);

        verify(schedulerJobRunRepository, never()).findByJobIdAndStatus(any(), any());
        verify(restExecutorService).execute(any(), any(SchedulerJobRun.class));
    }

    // ---------------------------------------------------------------------
    // Failure handling
    // ---------------------------------------------------------------------

    @Test
    void dispatch_executorThrows_marksFailureAndIncrementsConsecutiveFailures() {
        SchedulerJob job = activeJob(JobType.REST);
        job.setConsecutiveFailures(2);
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(schedulerJobRunRepository.findByJobIdAndStatus(jobId, JobRunStatus.RUNNING)).thenReturn(List.of());
        when(schedulerJobRunRepository.save(any(SchedulerJobRun.class))).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> { throw new RuntimeException("boom"); })
                .when(restExecutorService).execute(any(), any(SchedulerJobRun.class));

        service.dispatch(jobId);

        assertThat(job.getLastRunStatus()).isEqualTo(JobRunStatus.FAILURE);
        assertThat(job.getConsecutiveFailures()).isEqualTo(3);
    }

    // ---------------------------------------------------------------------
    // triggerManually
    // ---------------------------------------------------------------------

    @Test
    void triggerManually_found_dispatchesWithManualTrigger() {
        SchedulerJob job = activeJob(JobType.REST);
        when(schedulerJobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.of(job));
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(schedulerJobRunRepository.findByJobIdAndStatus(jobId, JobRunStatus.RUNNING)).thenReturn(List.of());
        when(schedulerJobRunRepository.save(any(SchedulerJobRun.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerJobRun run = service.triggerManually(jobId, tenantId, "tim");

        assertThat(run).isNotNull();
        assertThat(run.getTriggerType()).isEqualTo(JobTriggerType.MANUAL);
        assertThat(run.getTriggeredBy()).isEqualTo("tim");
    }

    @Test
    void triggerManually_notFoundForTenant_throwsRuntimeException() {
        when(schedulerJobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.triggerManually(jobId, tenantId, "tim"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Job not found or access denied");
    }
}
