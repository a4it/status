package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing an alert rule that fires when log counts exceed a threshold.
 * Rules are evaluated periodically against log_metrics data.
 */
@Entity
@Table(name = "alert_rules")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Service to match. Null means all services. */
    @Column(name = "service", length = 255)
    private String service;

    /** Log level to match. Null means all levels. */
    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "threshold_count", nullable = false)
    private Long thresholdCount;

    @Column(name = "window_minutes", nullable = false)
    private Integer windowMinutes;

    @Column(name = "cooldown_minutes")
    private Integer cooldownMinutes = 15;

    /** EMAIL, SLACK, or WEBHOOK */
    @Column(name = "notification_type", nullable = false, length = 20)
    private String notificationType;

    /** Email address, Slack webhook URL, or HTTP URL */
    @Column(name = "notification_target", columnDefinition = "TEXT")
    private String notificationTarget;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_fired_at")
    private ZonedDateTime lastFiredAt;

    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new alert rule.
     * Populates the creation timestamps if they have not been set explicitly.
     */
    @PrePersist
    public void prePersist() {
        // Default the human-readable creation timestamp when not already set
        if (createdDate == null) {
            createdDate = ZonedDateTime.now();
        }
        // Default the technical (epoch millis) creation timestamp when not already set
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
    }

    /**
     * Default constructor required by JPA.
     */
    public AlertRule() {
    }

    /** @return the unique identifier of the alert rule */
    public UUID getId() { return id; }
    /** @param id the unique identifier to set */
    public void setId(UUID id) { this.id = id; }

    /** @return the tenant that owns this alert rule */
    public Tenant getTenant() { return tenant; }
    /** @param tenant the owning tenant to set */
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    /** @return the display name of the alert rule */
    public String getName() { return name; }
    /** @param name the display name to set */
    public void setName(String name) { this.name = name; }

    /** @return the service to match, or null to match all services */
    public String getService() { return service; }
    /** @param service the service to match to set */
    public void setService(String service) { this.service = service; }

    /** @return the log level to match, or null to match all levels */
    public String getLevel() { return level; }
    /** @param level the log level to match to set */
    public void setLevel(String level) { this.level = level; }

    /** @return the threshold log count that triggers this rule */
    public Long getThresholdCount() { return thresholdCount; }
    /** @param thresholdCount the threshold log count to set */
    public void setThresholdCount(Long thresholdCount) { this.thresholdCount = thresholdCount; }

    /** @return the evaluation window in minutes */
    public Integer getWindowMinutes() { return windowMinutes; }
    /** @param windowMinutes the evaluation window in minutes to set */
    public void setWindowMinutes(Integer windowMinutes) { this.windowMinutes = windowMinutes; }

    /** @return the cooldown period in minutes between firings */
    public Integer getCooldownMinutes() { return cooldownMinutes; }
    /** @param cooldownMinutes the cooldown period in minutes to set */
    public void setCooldownMinutes(Integer cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }

    /** @return the notification type (EMAIL, SLACK, or WEBHOOK) */
    public String getNotificationType() { return notificationType; }
    /** @param notificationType the notification type to set */
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    /** @return the notification target (email, Slack webhook URL, or HTTP URL) */
    public String getNotificationTarget() { return notificationTarget; }
    /** @param notificationTarget the notification target to set */
    public void setNotificationTarget(String notificationTarget) { this.notificationTarget = notificationTarget; }

    /** @return true if the rule is active, false otherwise */
    public Boolean getIsActive() { return isActive; }
    /** @param isActive the active flag to set */
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    /** @return the timestamp when the rule last fired */
    public ZonedDateTime getLastFiredAt() { return lastFiredAt; }
    /** @param lastFiredAt the last-fired timestamp to set */
    public void setLastFiredAt(ZonedDateTime lastFiredAt) { this.lastFiredAt = lastFiredAt; }

    /** @return the creation timestamp */
    public ZonedDateTime getCreatedDate() { return createdDate; }
    /** @param createdDate the creation timestamp to set */
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }

    /** @return the technical creation timestamp in epoch milliseconds */
    public Long getCreatedDateTechnical() { return createdDateTechnical; }
    /** @param createdDateTechnical the technical creation timestamp to set */
    public void setCreatedDateTechnical(Long createdDateTechnical) { this.createdDateTechnical = createdDateTechnical; }
}
