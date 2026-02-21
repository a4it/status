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

    @PrePersist
    public void prePersist() {
        if (createdDate == null) {
            createdDate = ZonedDateTime.now();
        }
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
    }

    public AlertRule() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

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

    public Long getCreatedDateTechnical() { return createdDateTechnical; }
    public void setCreatedDateTechnical(Long createdDateTechnical) { this.createdDateTechnical = createdDateTechnical; }
}
