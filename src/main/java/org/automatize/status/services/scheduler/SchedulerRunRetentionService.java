package org.automatize.status.services.scheduler;

import org.automatize.status.repositories.SchedulerJobRepository;
import org.automatize.status.repositories.SchedulerJobRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

/**
 * Housekeeping service that purges old {@link org.automatize.status.models.SchedulerJobRun}
 * records according to configurable retention rules.
 *
 * <p>Runs automatically at 03:00 every day via a Spring cron trigger. Two retention
 * policies are applied per job:
 * <ol>
 *   <li>Delete runs older than {@code scheduler.run-retention-days} (default: 30 days)</li>
 *   <li>The repository's {@code findTop100} query already caps the live view to 100 runs;
 *       the cutoff-based delete is the primary enforcement mechanism</li>
 * </ol>
 * </p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Service
public class SchedulerRunRetentionService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerRunRetentionService.class);

    @Autowired private SchedulerJobRepository jobRepository;
    @Autowired private SchedulerJobRunRepository runRepository;

    @Value("${scheduler.run-retention-days:30}")
    private int retentionDays;

    /**
     * Deletes run records older than the configured retention period.
     * Runs daily at 03:00 server time.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanOldRuns() {
        logger.info("Starting scheduler run retention cleanup (retention: {} days)...", retentionDays);
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(retentionDays);

        jobRepository.findAll().forEach(job -> {
            try {
                runRepository.deleteByJobIdAndStartedAtBefore(job.getId(), cutoff);
            } catch (Exception e) {
                logger.error("Error cleaning runs for job {}: {}", job.getId(), e.getMessage());
            }
        });

        logger.info("Scheduler run retention cleanup completed");
    }
}
