package org.automatize.status.services.scheduler;

import jakarta.annotation.PostConstruct;
import org.automatize.status.models.SchedulerJob;
import org.automatize.status.models.scheduler.JobStatus;
import org.automatize.status.repositories.SchedulerJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Runtime lifecycle manager for the scheduler engine.
 *
 * <p>On application startup ({@link PostConstruct}) all enabled, ACTIVE jobs are
 * loaded from the database and registered as cron tasks with Spring's
 * {@link TaskScheduler}. Individual jobs can be registered, unregistered, or
 * rescheduled at runtime (e.g. when a job is created, updated, paused, or deleted
 * via the management API).</p>
 *
 * <p>The engine can be completely disabled at the application level via the
 * {@code scheduler.enabled} property (defaults to {@code true}).</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Service
public class SchedulerEngineService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerEngineService.class);

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private SchedulerJobRepository schedulerJobRepository;

    @Autowired
    private JobDispatcherService jobDispatcherService;

    @Value("${scheduler.enabled:true}")
    private boolean schedulerEnabled;

    /** Live map from job UUID to its pending {@link ScheduledFuture}. */
    private final Map<UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Initialises the engine by registering all currently active, enabled jobs.
     * Called automatically after Spring wires all dependencies.
     */
    @PostConstruct
    @Transactional(readOnly = true)
    public void init() {
        if (!schedulerEnabled) {
            logger.info("Scheduler engine is disabled via configuration");
            return;
        }
        logger.info("Initialising scheduler engine...");
        List<SchedulerJob> activeJobs = schedulerJobRepository.findByEnabledTrueAndStatus(JobStatus.ACTIVE);
        for (SchedulerJob job : activeJobs) {
            try {
                registerJob(job);
                logger.info("Registered job: {} ({})", job.getName(), job.getId());
            } catch (Exception e) {
                logger.error("Failed to register job: {} ({})", job.getName(), job.getId(), e);
            }
        }
        logger.info("Scheduler engine initialised with {} active job(s)", scheduledTasks.size());
    }

    /**
     * Registers a job with the task scheduler.
     * Any previously registered schedule for the same job ID is cancelled first.
     * No-ops when the job is disabled or not in ACTIVE status.
     *
     * @param job the job to schedule
     * @throws RuntimeException when the cron expression is invalid or scheduling fails
     */
    public void registerJob(SchedulerJob job) {
        unregisterJob(job.getId());
        if (!Boolean.TRUE.equals(job.getEnabled()) || job.getStatus() != JobStatus.ACTIVE) {
            return;
        }
        try {
            String timezone = job.getTimeZone() != null ? job.getTimeZone() : "UTC";
            CronTrigger trigger = new CronTrigger(job.getCronExpression(), ZoneId.of(timezone));
            UUID jobId = job.getId();
            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> jobDispatcherService.dispatch(jobId),
                    trigger
            );
            scheduledTasks.put(jobId, future);
        } catch (Exception e) {
            logger.error("Failed to register cron job {}: {}", job.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to schedule job: " + e.getMessage(), e);
        }
    }

    /**
     * Cancels and removes the scheduled task for the given job ID.
     * No-ops when the job has no active schedule.
     *
     * @param jobId the UUID of the job to unregister
     */
    public void unregisterJob(UUID jobId) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobId);
        if (future != null) {
            // false = do not interrupt a currently-running execution
            future.cancel(false);
        }
    }

    /**
     * Cancels any existing schedule for the job and immediately registers a new one.
     * Used when a job's cron expression or timezone has been updated.
     *
     * @param job the updated job to reschedule
     */
    public void rescheduleJob(SchedulerJob job) {
        unregisterJob(job.getId());
        registerJob(job);
    }

    /**
     * Returns the number of jobs currently registered with the task scheduler.
     *
     * @return active scheduled task count
     */
    public int getActiveJobCount() {
        return scheduledTasks.size();
    }

    /**
     * Returns {@code true} when the job has an active cron schedule registered.
     *
     * @param jobId the job UUID to check
     * @return {@code true} if scheduled
     */
    public boolean isJobScheduled(UUID jobId) {
        return scheduledTasks.containsKey(jobId);
    }
}
