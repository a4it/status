package org.automatize.status.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

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

    public AlertRuleRequest() {
    }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public long getThresholdCount() { return thresholdCount; }
    public void setThresholdCount(long thresholdCount) { this.thresholdCount = thresholdCount; }

    public int getWindowMinutes() { return windowMinutes; }
    public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }

    public int getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(int cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public String getNotificationTarget() { return notificationTarget; }
    public void setNotificationTarget(String notificationTarget) { this.notificationTarget = notificationTarget; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
