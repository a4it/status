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

@ExtendWith(MockitoExtension.class)
class SchedulerEngineServiceTest {

    @Mock private TaskScheduler taskScheduler;
    @Mock private SchedulerJobRepository schedulerJobRepository;
    @Mock private JobDispatcherService jobDispatcherService;
    @Mock private ScheduledFuture<?> scheduledFuture;

    @InjectMocks private SchedulerEngineService service;

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

    @BeforeEach
    void enableScheduler() {
        ReflectionTestUtils.setField(service, "schedulerEnabled", true);
    }

    // ---------------------------------------------------------------------
    // registerJob
    // ---------------------------------------------------------------------

    @Test
    void registerJob_activeEnabled_schedulesAndTracks() {
        SchedulerJob job = validJob();
        doReturnFuture();

        service.registerJob(job);

        assertThat(service.isJobScheduled(job.getId())).isTrue();
        assertThat(service.getActiveJobCount()).isEqualTo(1);
        verify(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void registerJob_disabled_doesNotSchedule() {
        SchedulerJob job = validJob();
        job.setEnabled(false);

        service.registerJob(job);

        assertThat(service.isJobScheduled(job.getId())).isFalse();
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void registerJob_notActive_doesNotSchedule() {
        SchedulerJob job = validJob();
        job.setStatus(JobStatus.PAUSED);

        service.registerJob(job);

        assertThat(service.isJobScheduled(job.getId())).isFalse();
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void registerJob_invalidTimezone_throwsJobSchedulingException() {
        SchedulerJob job = validJob();
        job.setTimeZone("Not/AZone");

        assertThatThrownBy(() -> service.registerJob(job))
                .isInstanceOf(JobSchedulingException.class)
                .hasMessageContaining("Failed to schedule job");
    }

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

    @Test
    void unregisterJob_scheduled_cancelsFuture() {
        SchedulerJob job = validJob();
        doReturnFuture();
        service.registerJob(job);

        service.unregisterJob(job.getId());

        verify(scheduledFuture).cancel(false);
        assertThat(service.isJobScheduled(job.getId())).isFalse();
    }

    @Test
    void unregisterJob_notScheduled_noOp() {
        service.unregisterJob(UUID.randomUUID());
        verify(scheduledFuture, never()).cancel(false);
    }

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

    @Test
    void init_disabled_doesNotQueryOrSchedule() {
        ReflectionTestUtils.setField(service, "schedulerEnabled", false);

        service.init();

        verify(schedulerJobRepository, never()).findByEnabledTrueAndStatus(any());
        assertThat(service.getActiveJobCount()).isZero();
    }

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

    private void doReturnFuture() {
        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class)))
                .thenAnswer(inv -> scheduledFuture);
    }
}
