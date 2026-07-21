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
import java.time.Duration;
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
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LogMetricService logMetricService;

    /**
     * Retrieves all alert rules ordered by creation date (newest first).
     *
     * @return the list of all persisted alert rules
     */
    @Transactional(readOnly = true)
    public List<AlertRule> findAll() {
        return alertRuleRepository.findAllByOrderByCreatedDateTechnicalDesc();
    }

    /**
     * Retrieves a single alert rule by its identifier.
     *
     * @param id the unique identifier of the alert rule
     * @return the matching alert rule
     * @throws RuntimeException if no alert rule exists for the given id
     */
    @Transactional(readOnly = true)
    public AlertRule findById(UUID id) {
        return alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert rule not found: " + id));
    }

    /**
     * Creates and persists a new alert rule.
     *
     * @param tenantId the tenant this rule belongs to, or {@code null} for no tenant association
     * @param name the human-readable name of the rule
     * @param service the service this rule monitors
     * @param level the log level this rule matches
     * @param thresholdCount the number of matching events that triggers the alert
     * @param windowMinutes the evaluation window in minutes
     * @param cooldownMinutes the cooldown period in minutes between consecutive alerts
     * @param notificationType the notification channel type (EMAIL, SLACK, or WEBHOOK)
     * @param notificationTarget the destination address or URL for the notification
     * @param active whether the rule is active upon creation
     * @return the persisted alert rule
     */
    public AlertRule create(UUID tenantId, String name, String service, String level,
                            long thresholdCount, int windowMinutes, int cooldownMinutes,
                            String notificationType, String notificationTarget, boolean active) {
        AlertRule rule = new AlertRule();
        // Associate the rule with a tenant only when a tenant id was supplied
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

    /**
     * Updates an existing alert rule with new field values and persists the change.
     *
     * @param id the unique identifier of the alert rule to update
     * @param name the new human-readable name of the rule
     * @param service the new service this rule monitors
     * @param level the new log level this rule matches
     * @param thresholdCount the new number of matching events that triggers the alert
     * @param windowMinutes the new evaluation window in minutes
     * @param cooldownMinutes the new cooldown period in minutes between consecutive alerts
     * @param notificationType the new notification channel type (EMAIL, SLACK, or WEBHOOK)
     * @param notificationTarget the new destination address or URL for the notification
     * @param active whether the rule should be active
     * @return the updated and persisted alert rule
     * @throws RuntimeException if no alert rule exists for the given id
     */
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

    /**
     * Deletes the alert rule identified by the given id.
     *
     * @param id the unique identifier of the alert rule to delete
     * @throws RuntimeException if no alert rule exists for the given id
     */
    public void delete(UUID id) {
        alertRuleRepository.delete(findById(id));
    }

    /**
     * Toggles the active state of the alert rule identified by the given id.
     *
     * @param id the unique identifier of the alert rule to toggle
     * @return the updated and persisted alert rule with its active state flipped
     * @throws RuntimeException if no alert rule exists for the given id
     */
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

    /**
     * Evaluates a single alert rule: skips it if still in cooldown, otherwise counts
     * matching events within the rule's window and fires an alert if the threshold is met.
     *
     * @param rule the alert rule to evaluate
     */
    private void evaluate(AlertRule rule) {
        // Skip evaluation while the rule is still within its cooldown period
        if (isInCooldown(rule)) {
            return;
        }

        ZonedDateTime since = ZonedDateTime.now().minusMinutes(rule.getWindowMinutes());
        long count = logMetricService.sumCountSince(rule.getService(), rule.getLevel(), since);

        // Fire the alert when the matching event count reaches the configured threshold
        if (count >= rule.getThresholdCount()) {
            logger.warn("Alert rule '{}' triggered: count={} >= threshold={}", rule.getName(), count, rule.getThresholdCount());
            fireAlert(rule, count);
            rule.setLastFiredAt(ZonedDateTime.now());
            alertRuleRepository.save(rule);
        }
    }

    /**
     * Determines whether the rule is currently within its cooldown period and should
     * therefore not fire again yet.
     *
     * @param rule the alert rule to check
     * @return {@code true} if the rule is still in cooldown, {@code false} otherwise
     */
    private boolean isInCooldown(AlertRule rule) {
        // Never fired or no cooldown configured means the rule is not in cooldown
        if (rule.getLastFiredAt() == null || rule.getCooldownMinutes() == null) {
            return false;
        }
        ZonedDateTime cooldownExpiry = rule.getLastFiredAt().plusMinutes(rule.getCooldownMinutes());
        return ZonedDateTime.now().isBefore(cooldownExpiry);
    }

    /**
     * Dispatches an alert notification through the channel configured on the rule.
     *
     * @param rule the alert rule that was triggered
     * @param count the number of matching events that triggered the alert
     */
    private void fireAlert(AlertRule rule, long count) {
        String subject = String.format("[Alert] %s — %d events in %d min", rule.getName(), count, rule.getWindowMinutes());
        String body = buildAlertBody(rule, count);

        // Route the notification to the channel matching the rule's configured type
        switch (rule.getNotificationType().toUpperCase()) {
            case "EMAIL" -> emailService.sendSimpleEmail(rule.getNotificationTarget(), subject, body);
            case "SLACK" -> sendSlackWebhook(rule.getNotificationTarget(), subject + "\n" + body);
            case "WEBHOOK" -> sendHttpWebhook(rule.getNotificationTarget(), subject, body);
            default -> logger.warn("Unknown notification type: {}", rule.getNotificationType());
        }
    }

    /**
     * Builds the human-readable body text describing a triggered alert.
     *
     * @param rule the alert rule that was triggered
     * @param count the number of matching events that triggered the alert
     * @return the formatted alert body text
     */
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

    /**
     * Sends a Slack-formatted message to the given Slack incoming webhook URL.
     *
     * @param webhookUrl the Slack incoming webhook URL
     * @param text the message text to send
     */
    private void sendSlackWebhook(String webhookUrl, String text) {
        String payload = "{\"text\": \"" + escapeJson(text) + "\"}";
        postJson(webhookUrl, payload);
    }

    /**
     * Sends a generic JSON webhook containing the alert subject and body.
     *
     * @param url the webhook endpoint URL
     * @param subject the alert subject
     * @param body the alert body
     */
    private void sendHttpWebhook(String url, String subject, String body) {
        String payload = String.format("{\"subject\":\"%s\",\"body\":\"%s\"}",
                escapeJson(subject), escapeJson(body));
        postJson(url, payload);
    }

    /**
     * Performs an HTTP POST with a JSON payload to the given URL, logging the response
     * or any failure. Restores the thread's interrupt status on interruption.
     *
     * @param url the target URL
     * @param jsonBody the JSON payload to send
     */
    private void postJson(String url, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Webhook response: {} {}", response.statusCode(), url);
        } catch (IOException | InterruptedException e) {
            logger.error("Webhook call failed to {}: {}", url, e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Escapes backslashes, double quotes, and newlines so the given text can be
     * safely embedded inside a JSON string literal.
     *
     * @param text the raw text to escape, may be {@code null}
     * @return the escaped text, or an empty string if the input was {@code null}
     */
    private String escapeJson(String text) {
        // Treat a null input as an empty escaped string
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
