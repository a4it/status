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

/**
 * Unit tests for {@link JobDispatcherService}, the component that loads a job,
 * enforces enabled/status/concurrency guards, routes execution to the correct
 * executor by {@link JobType}, and records the resulting {@link SchedulerJobRun}.
 *
 * <p>Collaborators (repositories and the per-type executor services) are Mockito
 * mocks injected into the service under test.</p>
 */
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

    /**
     * Builds an enabled, ACTIVE, non-concurrent job of the given type for use as test fixture.
     *
     * @param type the job type to assign
     * @return a ready-to-dispatch {@link SchedulerJob}
     */
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

    /**
     * Verifies a REST job is routed to the REST executor only.
     * Expected outcome: REST executor invoked, SQL executor not invoked, job saved.
     */
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

    /**
     * Verifies a SQL job is routed to the SQL executor only.
     * Expected outcome: SQL executor invoked, REST executor not invoked.
     */
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

    /**
     * Verifies a successful scheduled run is recorded with the correct trigger type and status.
     * Expected outcome: run is SCHEDULED/SUCCESS with a finish time, job reflects success and zero failures.
     */
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

    /**
     * Verifies a disabled job is skipped without executing or recording a run.
     * Expected outcome: no executor invocation and no run saved.
     */
    @Test
    void dispatch_disabledJob_skipsAndReturnsNull() {
        SchedulerJob job = activeJob(JobType.REST);
        job.setEnabled(false);
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        service.dispatch(jobId);

        verify(restExecutorService, never()).execute(any(), any());
        verify(schedulerJobRunRepository, never()).save(any());
    }

    /**
     * Verifies a non-ACTIVE (paused) job is skipped.
     * Expected outcome: no executor invocation and no run saved.
     */
    @Test
    void dispatch_notActiveJob_skips() {
        SchedulerJob job = activeJob(JobType.REST);
        job.setStatus(JobStatus.PAUSED);
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        service.dispatch(jobId);

        verify(restExecutorService, never()).execute(any(), any());
        verify(schedulerJobRunRepository, never()).save(any());
    }

    /**
     * Verifies dispatching an unknown job id is a no-op.
     * Expected outcome: no run is saved.
     */
    @Test
    void dispatch_jobNotFound_returnsNullNoExecution() {
        when(schedulerJobRepository.findById(jobId)).thenReturn(Optional.empty());

        service.dispatch(jobId);

        verify(schedulerJobRunRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // Concurrency guard
    // ---------------------------------------------------------------------

    /**
     * Verifies that when concurrency is disallowed and a run is already RUNNING, a SKIPPED run is recorded.
     * Expected outcome: a single run saved with status SKIPPED and no executor invocation.
     */
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

    /**
     * Verifies that when concurrency is allowed, execution proceeds without the running-check query.
     * Expected outcome: no RUNNING-status query and the executor is invoked.
     */
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

    /**
     * Verifies that an executor exception is caught, the run marked FAILURE, and the failure counter incremented.
     * Expected outcome: job last status FAILURE and consecutive failures bumped from 2 to 3.
     */
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

    /**
     * Verifies a manual trigger for an accessible job dispatches with MANUAL trigger metadata.
     * Expected outcome: a non-null run with trigger type MANUAL and the triggering user recorded.
     */
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

    /**
     * Verifies a manual trigger for a job not owned by the tenant is rejected.
     * Expected outcome: a {@link RuntimeException} with an access-denied message.
     */
    @Test
    void triggerManually_notFoundForTenant_throwsRuntimeException() {
        when(schedulerJobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.triggerManually(jobId, tenantId, "tim"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Job not found or access denied");
    }
}
