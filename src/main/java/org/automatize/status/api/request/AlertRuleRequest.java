package org.automatize.status.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Request payload used to create or update an alert rule in the status-monitoring app.
 *
 * <p>An alert rule defines a threshold condition over incoming log/event volume for a
 * given tenant, service and severity level. When the number of matching entries reaches
 * {@code thresholdCount} within a rolling {@code windowMinutes} window, a notification of
 * the configured {@code notificationType} is dispatched to {@code notificationTarget},
 * respecting the {@code cooldownMinutes} suppression period between successive alerts.</p>
 */
public class AlertRuleRequest {

    private UUID tenantId;

    @NotBlank(message = "Name is required")
    private String name;

    private String service;

    private String level;

    @Min(value = 1, message = "Threshold must be at least 1")
    private long thresholdCount;

    @Min(value = 1, message = "Window must be at least 1 minute")
    private int windowMinutes;

    private int cooldownMinutes = 15;

    @NotBlank(message = "Notification type is required")
    @Pattern(regexp = "^(EMAIL|SLACK|WEBHOOK)$", message = "Notification type must be EMAIL, SLACK, or WEBHOOK")
    private String notificationType;

    private String notificationTarget;

    private boolean active = true;

    /**
     * Creates an empty alert rule request for framework/deserialization use.
     */
    public AlertRuleRequest() {
    }

    /** @return the tenant this alert rule belongs to */
    public UUID getTenantId() { return tenantId; }
    /** @param tenantId the owning tenant identifier to set */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    /** @return the human-readable name of the alert rule */
    public String getName() { return name; }
    /** @param name the alert rule name to set */
    public void setName(String name) { this.name = name; }

    /** @return the service the rule is scoped to, or {@code null} for all services */
    public String getService() { return service; }
    /** @param service the service scope to set */
    public void setService(String service) { this.service = service; }

    /** @return the severity level the rule matches, or {@code null} for all levels */
    public String getLevel() { return level; }
    /** @param level the severity level to set */
    public void setLevel(String level) { this.level = level; }

    /** @return the number of matching entries that triggers the alert */
    public long getThresholdCount() { return thresholdCount; }
    /** @param thresholdCount the trigger threshold count to set */
    public void setThresholdCount(long thresholdCount) { this.thresholdCount = thresholdCount; }

    /** @return the rolling evaluation window in minutes */
    public int getWindowMinutes() { return windowMinutes; }
    /** @param windowMinutes the evaluation window in minutes to set */
    public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }

    /** @return the suppression period in minutes between successive alerts */
    public int getCooldownMinutes() { return cooldownMinutes; }
    /** @param cooldownMinutes the cooldown period in minutes to set */
    public void setCooldownMinutes(int cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }

    /** @return the notification channel type (EMAIL, SLACK, or WEBHOOK) */
    public String getNotificationType() { return notificationType; }
    /** @param notificationType the notification channel type to set */
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    /** @return the destination for the notification (address, channel, or URL) */
    public String getNotificationTarget() { return notificationTarget; }
    /** @param notificationTarget the notification destination to set */
    public void setNotificationTarget(String notificationTarget) { this.notificationTarget = notificationTarget; }

    /** @return {@code true} if the rule is active and should be evaluated */
    public boolean isActive() { return active; }
    /** @param active whether the rule is active */
    public void setActive(boolean active) { this.active = active; }
}
