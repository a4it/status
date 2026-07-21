package org.automatize.status.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduler that aggregates raw log counts into the log_metrics table every minute.
 * This allows dashboards and alert rules to query pre-aggregated data instead of
 * performing expensive full-table scans on the logs table.
 */
@Service
public class LogMetricScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LogMetricScheduler.class);

    @Autowired
    private LogMetricService logMetricService;

    /**
     * Scheduled task that runs every minute to aggregate recent raw logs into
     * the log_metrics table. Any failure is logged and swallowed so that a
     * single failed run does not stop future scheduled executions.
     */
    @Scheduled(cron = "0 * * * * *")
    public void aggregate() {
        try {
            logMetricService.aggregateRecentLogs();
        } catch (Exception e) {
            logger.error("Log metric aggregation failed: {}", e.getMessage(), e);
        }
    }
}
