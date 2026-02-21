package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

public class AlertRuleResponse {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String service;
    private String level;
    private Long thresholdCount;
    private Integer windowMinutes;
    private Integer cooldownMinutes;
    private String notificationType;
    private String notificationTarget;
    private Boolean isActive;
    private ZonedDateTime lastFiredAt;
    private ZonedDateTime createdDate;

    public AlertRuleResponse() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Long getThresholdCount() { return thresholdCount; }
    public void setThresholdCount(Long thresholdCount) { this.thresholdCount = thresholdCount; }

    public Integer getWindowMinutes() { return windowMinutes; }
    public void setWindowMinutes(Integer windowMinutes) { this.windowMinutes = windowMinutes; }

    public Integer getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(Integer cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public String getNotificationTarget() { return notificationTarget; }
    public void setNotificationTarget(String notificationTarget) { this.notificationTarget = notificationTarget; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public ZonedDateTime getLastFiredAt() { return lastFiredAt; }
    public void setLastFiredAt(ZonedDateTime lastFiredAt) { this.lastFiredAt = lastFiredAt; }

    public ZonedDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }
}
