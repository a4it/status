package org.automatize.status.services;

import org.automatize.status.models.AlertRule;
import org.automatize.status.repositories.AlertRuleRepository;
import org.automatize.status.repositories.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing alert rules and dispatching notifications when thresholds are breached.
 */
@Service
@Transactional
public class AlertRuleService {

    private static final Logger logger = LoggerFactory.getLogger(AlertRuleService.class);

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LogMetricService logMetricService;

    @Transactional(readOnly = true)
    public List<AlertRule> findAll() {
        return alertRuleRepository.findAllByOrderByCreatedDateTechnicalDesc();
    }

    @Transactional(readOnly = true)
    public AlertRule findById(UUID id) {
        return alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert rule not found: " + id));
    }

    public AlertRule create(UUID tenantId, String name, String service, String level,
                            long thresholdCount, int windowMinutes, int cooldownMinutes,
                            String notificationType, String notificationTarget, boolean active) {
        AlertRule rule = new AlertRule();
        if (tenantId != null) {
            tenantRepository.findById(tenantId).ifPresent(rule::setTenant);
        }
        rule.setName(name);
        rule.setService(service);
        rule.setLevel(level);
        rule.setThresholdCount(thresholdCount);
        rule.setWindowMinutes(windowMinutes);
        rule.setCooldownMinutes(cooldownMinutes);
        rule.setNotificationType(notificationType);
        rule.setNotificationTarget(notificationTarget);
        rule.setIsActive(active);
        return alertRuleRepository.save(rule);
    }

    public AlertRule update(UUID id, String name, String service, String level,
                            long thresholdCount, int windowMinutes, int cooldownMinutes,
                            String notificationType, String notificationTarget, boolean active) {
        AlertRule rule = findById(id);
        rule.setName(name);
        rule.setService(service);
        rule.setLevel(level);
        rule.setThresholdCount(thresholdCount);
        rule.setWindowMinutes(windowMinutes);
        rule.setCooldownMinutes(cooldownMinutes);
        rule.setNotificationType(notificationType);
        rule.setNotificationTarget(notificationTarget);
        rule.setIsActive(active);
        return alertRuleRepository.save(rule);
    }

    public void delete(UUID id) {
        alertRuleRepository.delete(findById(id));
    }

    public AlertRule toggleActive(UUID id) {
        AlertRule rule = findById(id);
        rule.setIsActive(!Boolean.TRUE.equals(rule.getIsActive()));
        return alertRuleRepository.save(rule);
    }

    /**
     * Evaluates all active alert rules against the log_metrics data.
     * Called periodically by AlertEvaluatorScheduler.
     */
    public void evaluateAll() {
        List<AlertRule> activeRules = alertRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc();
        for (AlertRule rule : activeRules) {
            try {
                evaluate(rule);
            } catch (Exception e) {
                logger.error("Error evaluating alert rule '{}': {}", rule.getName(), e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------

    private void evaluate(AlertRule rule) {
        if (isInCooldown(rule)) {
            return;
        }

        ZonedDateTime since = ZonedDateTime.now().minusMinutes(rule.getWindowMinutes());
        long count = logMetricService.sumCountSince(rule.getService(), rule.getLevel(), since);

        if (count >= rule.getThresholdCount()) {
            logger.warn("Alert rule '{}' triggered: count={} >= threshold={}", rule.getName(), count, rule.getThresholdCount());
            fireAlert(rule, count);
            rule.setLastFiredAt(ZonedDateTime.now());
            alertRuleRepository.save(rule);
        }
    }

    private boolean isInCooldown(AlertRule rule) {
        if (rule.getLastFiredAt() == null || rule.getCooldownMinutes() == null) {
            return false;
        }
        ZonedDateTime cooldownExpiry = rule.getLastFiredAt().plusMinutes(rule.getCooldownMinutes());
        return ZonedDateTime.now().isBefore(cooldownExpiry);
    }

    private void fireAlert(AlertRule rule, long count) {
        String subject = String.format("[Alert] %s — %d events in %d min", rule.getName(), count, rule.getWindowMinutes());
        String body = buildAlertBody(rule, count);

        switch (rule.getNotificationType().toUpperCase()) {
            case "EMAIL" -> emailService.sendSimpleEmail(rule.getNotificationTarget(), subject, body);
            case "SLACK" -> sendSlackWebhook(rule.getNotificationTarget(), subject + "\n" + body);
            case "WEBHOOK" -> sendHttpWebhook(rule.getNotificationTarget(), subject, body);
            default -> logger.warn("Unknown notification type: {}", rule.getNotificationType());
        }
    }

    private String buildAlertBody(AlertRule rule, long count) {
        return String.format(
                "Alert rule: %s\nService: %s\nLevel: %s\nCount: %d (threshold: %d)\nWindow: %d minutes\nTime: %s",
                rule.getName(),
                rule.getService() != null ? rule.getService() : "ALL",
                rule.getLevel() != null ? rule.getLevel() : "ALL",
                count,
                rule.getThresholdCount(),
                rule.getWindowMinutes(),
                ZonedDateTime.now()
        );
    }

    private void sendSlackWebhook(String webhookUrl, String text) {
        String payload = "{\"text\": \"" + escapeJson(text) + "\"}";
        postJson(webhookUrl, payload);
    }

    private void sendHttpWebhook(String url, String subject, String body) {
        String payload = String.format("{\"subject\":\"%s\",\"body\":\"%s\"}",
                escapeJson(subject), escapeJson(body));
        postJson(url, payload);
    }

    private void postJson(String url, String jsonBody) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Webhook response: {} {}", response.statusCode(), url);
        } catch (IOException | InterruptedException e) {
            logger.error("Webhook call failed to {}: {}", url, e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
