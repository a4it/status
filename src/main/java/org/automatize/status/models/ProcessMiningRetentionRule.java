package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing a per-platform log retention rule for process mining data.
 * Rules can target a specific platform within a tenant, or all platforms in a tenant
 * when platform is null.
 */
@Entity
@Table(name = "process_mining_retention_rules")
public class ProcessMiningRetentionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id")
    private StatusPlatform platform;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays = 30;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "last_run_at")
    private ZonedDateTime lastRunAt;

    @Column(name = "last_run_deleted_count")
    private Integer lastRunDeletedCount;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    public ProcessMiningRetentionRule() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public StatusPlatform getPlatform() { return platform; }
    public void setPlatform(StatusPlatform platform) { this.platform = platform; }

    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public ZonedDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(ZonedDateTime lastRunAt) { this.lastRunAt = lastRunAt; }

    public Integer getLastRunDeletedCount() { return lastRunDeletedCount; }
    public void setLastRunDeletedCount(Integer lastRunDeletedCount) { this.lastRunDeletedCount = lastRunDeletedCount; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
