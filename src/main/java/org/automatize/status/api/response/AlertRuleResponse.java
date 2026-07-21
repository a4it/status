package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Response object containing alert rule details.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Expose an alert rule's matching criteria (service, level, threshold and window)</li>
 *   <li>Describe how and where a triggered alert is delivered (notification type and target)</li>
 *   <li>Report the rule's activation state and when it last fired for cooldown tracking</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
public class AlertRuleResponse {

    /** The unique identifier of the alert rule. */
    private UUID id;

    /** The identifier of the owning tenant. */
    private UUID tenantId;

    /** The display name of the alert rule. */
    private String name;

    /** The service the rule matches against. */
    private String service;

    /** The log level the rule matches against. */
    private String level;

    /** The number of matching events required to trigger the rule. */
    private Long thresholdCount;

    /** The rolling evaluation window, in minutes. */
    private Integer windowMinutes;

    /** The minimum time between successive firings, in minutes. */
    private Integer cooldownMinutes;

    /** The type of notification sent when the rule fires. */
    private String notificationType;

    /** The destination the notification is delivered to. */
    private String notificationTarget;

    /** Whether the rule is currently active. */
    private Boolean isActive;

    /** When the rule last fired. */
    private ZonedDateTime lastFiredAt;

    /** When the rule was created. */
    private ZonedDateTime createdDate;

    /**
     * Default constructor.
     */
    public AlertRuleResponse() {
    }

    /** Gets the ID. @return the ID */
    public UUID getId() { return id; }
    /** Sets the ID. @param id the ID to set */
    public void setId(UUID id) { this.id = id; }

    /** Gets the tenant ID. @return the tenant ID */
    public UUID getTenantId() { return tenantId; }
    /** Sets the tenant ID. @param tenantId the tenant ID to set */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    /** Gets the name. @return the name */
    public String getName() { return name; }
    /** Sets the name. @param name the name to set */
    public void setName(String name) { this.name = name; }

    /** Gets the service. @return the service */
    public String getService() { return service; }
    /** Sets the service. @param service the service to set */
    public void setService(String service) { this.service = service; }

    /** Gets the level. @return the level */
    public String getLevel() { return level; }
    /** Sets the level. @param level the level to set */
    public void setLevel(String level) { this.level = level; }

    /** Gets the threshold count. @return the threshold count */
    public Long getThresholdCount() { return thresholdCount; }
    /** Sets the threshold count. @param thresholdCount the threshold count to set */
    public void setThresholdCount(Long thresholdCount) { this.thresholdCount = thresholdCount; }

    /** Gets the window in minutes. @return the window in minutes */
    public Integer getWindowMinutes() { return windowMinutes; }
    /** Sets the window in minutes. @param windowMinutes the window in minutes to set */
    public void setWindowMinutes(Integer windowMinutes) { this.windowMinutes = windowMinutes; }

    /** Gets the cooldown in minutes. @return the cooldown in minutes */
    public Integer getCooldownMinutes() { return cooldownMinutes; }
    /** Sets the cooldown in minutes. @param cooldownMinutes the cooldown in minutes to set */
    public void setCooldownMinutes(Integer cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }

    /** Gets the notification type. @return the notification type */
    public String getNotificationType() { return notificationType; }
    /** Sets the notification type. @param notificationType the notification type to set */
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    /** Gets the notification target. @return the notification target */
    public String getNotificationTarget() { return notificationTarget; }
    /** Sets the notification target. @param notificationTarget the notification target to set */
    public void setNotificationTarget(String notificationTarget) { this.notificationTarget = notificationTarget; }

    /** Gets the active status. @return true if active, false otherwise */
    public Boolean getIsActive() { return isActive; }
    /** Sets the active status. @param isActive the active status to set */
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    /** Gets the last fired time. @return the last fired time */
    public ZonedDateTime getLastFiredAt() { return lastFiredAt; }
    /** Sets the last fired time. @param lastFiredAt the last fired time to set */
    public void setLastFiredAt(ZonedDateTime lastFiredAt) { this.lastFiredAt = lastFiredAt; }

    /** Gets the creation date. @return the creation date */
    public ZonedDateTime getCreatedDate() { return createdDate; }
    /** Sets the creation date. @param createdDate the creation date to set */
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }
}
