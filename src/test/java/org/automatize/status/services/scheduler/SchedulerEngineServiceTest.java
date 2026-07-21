package org.automatize.status.services.scheduler;

import org.automatize.status.exceptions.JobSchedulingException;
import org.automatize.status.models.SchedulerJob;
import org.automatize.status.models.scheduler.JobStatus;
import org.automatize.status.models.scheduler.JobType;
import org.automatize.status.repositories.SchedulerJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SchedulerEngineService}, which registers/unregisters cron
 * triggers on a Spring {@link TaskScheduler}, tracks scheduled futures, and boots
 * active jobs on startup via {@code init}.
 *
 * <p>The {@link TaskScheduler}, repository and {@link ScheduledFuture} are Mockito
 * mocks; the {@code schedulerEnabled} flag is toggled through reflection.</p>
 */
@ExtendWith(MockitoExtension.class)
class SchedulerEngineServiceTest {

    @Mock private TaskScheduler taskScheduler;
    @Mock private SchedulerJobRepository schedulerJobRepository;
    @Mock private JobDispatcherService jobDispatcherService;
    @Mock private ScheduledFuture<?> scheduledFuture;

    @InjectMocks private SchedulerEngineService service;

    /**
     * Builds a valid enabled, ACTIVE REST job with a sound cron expression and UTC zone.
     *
     * @return a schedulable {@link SchedulerJob} fixture
     */
    private SchedulerJob validJob() {
        SchedulerJob job = new SchedulerJob();
        job.setId(UUID.randomUUID());
        job.setName("job");
        job.setJobType(JobType.REST);
        job.setCronExpression("0 0 3 * * *");
        job.setTimeZone("UTC");
        job.setEnabled(true);
        job.setStatus(JobStatus.ACTIVE);
        return job;
    }

    /**
     * Enables the scheduler flag via reflection before each test so registration paths are active.
     */
    @BeforeEach
    void enableScheduler() {
        ReflectionTestUtils.setField(service, "schedulerEnabled", true);
    }

    // ---------------------------------------------------------------------
    // registerJob
    // ---------------------------------------------------------------------

    /**
     * Verifies an enabled, ACTIVE job is scheduled and tracked.
     * Expected outcome: the job is marked scheduled, the active count is 1, and the scheduler is invoked.
     */
    @Test
    void registerJob_activeEnabled_schedulesAndTracks() {
        SchedulerJob job = validJob();
        doReturnFuture();

        service.registerJob(job);

        assertThat(service.isJobScheduled(job.getId())).isTrue();
        assertThat(service.getActiveJobCount()).isEqualTo(1);
        verify(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));
    }

    /**
     * Verifies a disabled job is not scheduled.
     * Expected outcome: the job is not tracked and the scheduler is never invoked.
     */
    @Test
    void registerJob_disabled_doesNotSchedule() {
        SchedulerJob job = validJob();
        job.setEnabled(false);

        service.registerJob(job);

        assertThat(service.isJobScheduled(job.getId())).isFalse();
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
    }

    /**
     * Verifies a non-ACTIVE (paused) job is not scheduled.
     * Expected outcome: the job is not tracked and the scheduler is never invoked.
     */
    @Test
    void registerJob_notActive_doesNotSchedule() {
        SchedulerJob job = validJob();
        job.setStatus(JobStatus.PAUSED);

        service.registerJob(job);

        assertThat(service.isJobScheduled(job.getId())).isFalse();
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
    }

    /**
     * Verifies an invalid timezone causes registration to fail.
     * Expected outcome: a {@link JobSchedulingException} with a "Failed to schedule job" message.
     */
    @Test
    void registerJob_invalidTimezone_throwsJobSchedulingException() {
        SchedulerJob job = validJob();
        job.setTimeZone("Not/AZone");

        assertThatThrownBy(() -> service.registerJob(job))
                .isInstanceOf(JobSchedulingException.class)
                .hasMessageContaining("Failed to schedule job");
    }

    /**
     * Verifies an invalid cron expression causes registration to fail.
     * Expected outcome: a {@link JobSchedulingException} is thrown.
     */
    @Test
    void registerJob_invalidCron_throwsJobSchedulingException() {
        SchedulerJob job = validJob();
        job.setCronExpression("this is not cron");

        assertThatThrownBy(() -> service.registerJob(job))
                .isInstanceOf(JobSchedulingException.class);
    }

    // ---------------------------------------------------------------------
    // unregisterJob / rescheduleJob
    // ---------------------------------------------------------------------

    /**
     * Verifies unregistering a scheduled job cancels its future and clears tracking.
     * Expected outcome: the future is cancelled and the job is no longer scheduled.
     */
    @Test
    void unregisterJob_scheduled_cancelsFuture() {
        SchedulerJob job = validJob();
        doReturnFuture();
        service.registerJob(job);

        service.unregisterJob(job.getId());

        verify(scheduledFuture).cancel(false);
        assertThat(service.isJobScheduled(job.getId())).isFalse();
    }

    /**
     * Verifies unregistering an unknown job id is a no-op.
     * Expected outcome: no future is cancelled.
     */
    @Test
    void unregisterJob_notScheduled_noOp() {
        service.unregisterJob(UUID.randomUUID());
        verify(scheduledFuture, never()).cancel(false);
    }

    /**
     * Verifies rescheduling re-registers the job with the scheduler.
     * Expected outcome: the job is scheduled and the scheduler is invoked.
     */
    @Test
    void rescheduleJob_reRegistersJob() {
        SchedulerJob job = validJob();
        doReturnFuture();

        service.rescheduleJob(job);

        assertThat(service.isJobScheduled(job.getId())).isTrue();
        verify(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));
    }

    // ---------------------------------------------------------------------
    // init
    // ---------------------------------------------------------------------

    /**
     * Verifies startup does nothing when the scheduler is disabled.
     * Expected outcome: no repository query and zero active jobs.
     */
    @Test
    void init_disabled_doesNotQueryOrSchedule() {
        ReflectionTestUtils.setField(service, "schedulerEnabled", false);

        service.init();

        verify(schedulerJobRepository, never()).findByEnabledTrueAndStatus(any());
        assertThat(service.getActiveJobCount()).isZero();
    }

    /**
     * Verifies startup registers every enabled, ACTIVE job returned by the repository.
     * Expected outcome: both jobs are scheduled and the active count is 2.
     */
    @Test
    void init_enabled_registersAllActiveJobs() {
        SchedulerJob job1 = validJob();
        SchedulerJob job2 = validJob();
        when(schedulerJobRepository.findByEnabledTrueAndStatus(JobStatus.ACTIVE))
                .thenReturn(List.of(job1, job2));
        doReturnFuture();

        service.init();

        assertThat(service.getActiveJobCount()).isEqualTo(2);
        assertThat(service.isJobScheduled(job1.getId())).isTrue();
        assertThat(service.isJobScheduled(job2.getId())).isTrue();
    }

    /**
     * Verifies startup keeps going when one job fails to register.
     * Expected outcome: the good job is scheduled while the bad job (invalid zone) is not.
     */
    @Test
    void init_enabled_continuesWhenOneJobFailsToRegister() {
        SchedulerJob good = validJob();
        SchedulerJob bad = validJob();
        bad.setTimeZone("Not/AZone");
        when(schedulerJobRepository.findByEnabledTrueAndStatus(JobStatus.ACTIVE))
                .thenReturn(List.of(bad, good));
        doReturnFuture();

        service.init();

        // bad job throws inside init and is caught; good job is still registered
        assertThat(service.isJobScheduled(good.getId())).isTrue();
        assertThat(service.isJobScheduled(bad.getId())).isFalse();
    }

    /**
     * Stubs the task scheduler to return the mock {@link ScheduledFuture} on any schedule call.
     */
    private void doReturnFuture() {
        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class)))
                .thenAnswer(inv -> scheduledFuture);
    }
}
