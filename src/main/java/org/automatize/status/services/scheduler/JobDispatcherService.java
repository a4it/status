package org.automatize.status.services.scheduler;

import org.automatize.status.models.SchedulerJob;
import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.JobStatus;
import org.automatize.status.models.scheduler.JobTriggerType;
import org.automatize.status.repositories.SchedulerJobRepository;
import org.automatize.status.repositories.SchedulerJobRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dispatcher service that sits between the scheduler engine (cron triggers) and the
 * type-specific executor services.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Guard against concurrent execution when the job disallows it</li>
 *   <li>Create and persist {@link SchedulerJobRun} records before and after execution</li>
 *   <li>Delegate to the correct executor based on {@link org.automatize.status.models.scheduler.JobType}</li>
 *   <li>Update job-level statistics (last run, consecutive failure count) after each run</li>
 * </ul>
 * </p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Service
public class JobDispatcherService {

    private static final Logger logger = LoggerFactory.getLogger(JobDispatcherService.class);

    @Autowired
    private SchedulerJobRepository schedulerJobRepository;

    @Autowired
    private SchedulerJobRunRepository schedulerJobRunRepository;

    @Autowired
    private ProgramExecutorService programExecutorService;

    @Autowired
    private SqlExecutorService sqlExecutorService;

    @Autowired
    private RestExecutorService restExecutorService;

    @Autowired
    private SoapExecutorService soapExecutorService;

    /**
     * Called by {@link SchedulerEngineService} when a cron trigger fires.
     *
     * @param jobId the UUID of the job to dispatch
     */
    @Transactional
    public void dispatch(UUID jobId) {
        dispatchInternal(jobId, JobTriggerType.SCHEDULED, null, 1);
    }

    /**
     * Manually triggers a job from the API, bypassing the cron schedule.
     * The job must belong to the given tenant.
     *
     * @param jobId    the job UUID to trigger
     * @param tenantId the tenant scope for security
     * @param username the username of the operator requesting the run
     * @return the created {@link SchedulerJobRun} record, or {@code null} when skipped
     */
    @Transactional
    public SchedulerJobRun triggerManually(UUID jobId, UUID tenantId, String username) {
        schedulerJobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new RuntimeException("Job not found or access denied"));
        return dispatchInternal(jobId, JobTriggerType.MANUAL, username, 1);
    }

    // -------------------------------------------------------------------------
    // Internal dispatch pipeline
    // -------------------------------------------------------------------------

    @Transactional
    protected SchedulerJobRun dispatchInternal(UUID jobId, JobTriggerType triggerType,
                                               String triggeredBy, int attemptNumber) {
        Optional<SchedulerJob> jobOpt = schedulerJobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            logger.warn("Job {} not found — skipping dispatch", jobId);
            return null;
        }
        SchedulerJob job = jobOpt.get();

        if (!Boolean.TRUE.equals(job.getEnabled()) || job.getStatus() != JobStatus.ACTIVE) {
            logger.debug("Job {} is not active/enabled — skipping", jobId);
            return null;
        }

        // Concurrency guard
        if (!Boolean.TRUE.equals(job.getAllowConcurrent())) {
            List<SchedulerJobRun> running = schedulerJobRunRepository.findByJobIdAndStatus(jobId, JobRunStatus.RUNNING);
            if (!running.isEmpty()) {
                logger.info("Job {} already running (concurrent not allowed) — recording SKIPPED run", jobId);
                return createSkippedRun(job, triggerType, triggeredBy, attemptNumber);
            }
        }

        // Create the in-progress run record
        SchedulerJobRun run = new SchedulerJobRun();
        run.setJob(job);
        run.setTenant(job.getTenant());
        run.setTriggerType(triggerType);
        run.setStatus(JobRunStatus.RUNNING);
        run.setAttemptNumber(attemptNumber);
        run.setStartedAt(ZonedDateTime.now());
        run.setTriggeredBy(triggeredBy);
        run.setCreatedDateTechnical(System.currentTimeMillis());
        run = schedulerJobRunRepository.save(run);

        executeJob(job, run);
        return run;
    }

    private void executeJob(SchedulerJob job, SchedulerJobRun run) {
        long startMs = System.currentTimeMillis();
        try {
            switch (job.getJobType()) {
                case PROGRAM -> programExecutorService.execute(job.getProgramConfig(), run);
                case SQL     -> sqlExecutorService.execute(job.getSqlConfig(), run);
                case REST    -> restExecutorService.execute(job.getRestConfig(), run);
                case SOAP    -> soapExecutorService.execute(job.getSoapConfig(), run);
            }
        } catch (Exception e) {
            logger.error("Unhandled exception during job execution for job {}: {}", job.getId(), e.getMessage(), e);
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage(e.getMessage());
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            run.setFinishedAt(ZonedDateTime.now());
            run.setDurationMs(durationMs);
            schedulerJobRunRepository.save(run);

            // Update job-level stats
            job.setLastRunAt(run.getFinishedAt());
            job.setLastRunStatus(run.getStatus());
            if (run.getStatus() == JobRunStatus.FAILURE || run.getStatus() == JobRunStatus.TIMEOUT) {
                job.setConsecutiveFailures(job.getConsecutiveFailures() + 1);
            } else if (run.getStatus() == JobRunStatus.SUCCESS) {
                job.setConsecutiveFailures(0);
            }
            schedulerJobRepository.save(job);
        }
    }

    private SchedulerJobRun createSkippedRun(SchedulerJob job, JobTriggerType triggerType,
                                              String triggeredBy, int attemptNumber) {
        SchedulerJobRun run = new SchedulerJobRun();
        run.setJob(job);
        run.setTenant(job.getTenant());
        run.setTriggerType(triggerType);
        run.setStatus(JobRunStatus.SKIPPED);
        run.setAttemptNumber(attemptNumber);
        run.setStartedAt(ZonedDateTime.now());
        run.setFinishedAt(ZonedDateTime.now());
        run.setDurationMs(0L);
        run.setTriggeredBy(triggeredBy);
        run.setCreatedDateTechnical(System.currentTimeMillis());
        return schedulerJobRunRepository.save(run);
    }
}
