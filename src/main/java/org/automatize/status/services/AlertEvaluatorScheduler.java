package org.automatize.status.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduler that evaluates all active alert rules every minute against log_metrics data.
 * When a rule's threshold is breached and the cooldown has expired, a notification
 * is dispatched via the configured channel (EMAIL, SLACK, or WEBHOOK).
 */
@Service
public class AlertEvaluatorScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AlertEvaluatorScheduler.class);

    @Autowired
    private AlertRuleService alertRuleService;

    @Scheduled(cron = "30 * * * * *")
    public void evaluate() {
        try {
            alertRuleService.evaluateAll();
        } catch (Exception e) {
            logger.error("Alert evaluation failed: {}", e.getMessage(), e);
        }
    }
}
