package org.automatize.status.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduler that purges log entries older than the configured retention period.
 * Runs once per day at 02:00 to keep the logs table from growing unboundedly.
 * The retention period is configured via {@code logs.retention.days} (default: 30).
 */
@Service
public class LogRetentionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LogRetentionScheduler.class);

    @Autowired
    private LogIngestionService logIngestionService;

    @Scheduled(cron = "0 0 2 * * *")
    public void purge() {
        logger.info("Starting log retention purge...");
        logIngestionService.purgeOldLogs();
    }
}
