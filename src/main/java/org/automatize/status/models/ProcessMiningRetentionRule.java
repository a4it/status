package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing a per-platform log retention rule for process mining data.
 * Rules can target a specific platform within a tenant, or all platforms in a tenant
 * when platform is null.
 *
 * <p>Each rule records how long process mining data should be kept
 * ({@code retentionDays}), whether the rule is active, and bookkeeping about the
 * most recent cleanup run (timestamp and deleted-row count).</p>
 *
 * @see Tenant
 * @see StatusPlatform
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

    /**
     * JPA lifecycle callback executed before persisting a new retention rule.
     * Populates the creation and update timestamps when they have not been set.
     */
    @PrePersist
    public void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        // Set the creation timestamp only if it has not already been assigned.
        if (createdAt == null) {
            createdAt = now;
        }
        // Set the update timestamp only if it has not already been assigned.
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * JPA lifecycle callback executed before updating an existing retention rule.
     * Refreshes the update timestamp to the current time.
     */
    @PreUpdate
    public void preUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    /**
     * Default constructor required by JPA.
     */
    public ProcessMiningRetentionRule() {
    }

    /**
     * Gets the unique identifier of the retention rule.
     *
     * @return the UUID of the rule
     */
    public UUID getId() { return id; }

    /**
     * Sets the unique identifier of the retention rule.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) { this.id = id; }

    /**
     * Gets the tenant this rule belongs to.
     *
     * @return the associated {@link Tenant}
     */
    public Tenant getTenant() { return tenant; }

    /**
     * Sets the tenant this rule belongs to.
     *
     * @param tenant the {@link Tenant} to associate
     */
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    /**
     * Gets the platform this rule targets, or {@code null} if it applies to all platforms.
     *
     * @return the associated {@link StatusPlatform}, or {@code null}
     */
    public StatusPlatform getPlatform() { return platform; }

    /**
     * Sets the platform this rule targets.
     *
     * @param platform the {@link StatusPlatform} to associate, or {@code null} for all platforms
     */
    public void setPlatform(StatusPlatform platform) { this.platform = platform; }

    /**
     * Gets the number of days data is retained before being eligible for deletion.
     *
     * @return the retention period in days
     */
    public int getRetentionDays() { return retentionDays; }

    /**
     * Sets the number of days data is retained before being eligible for deletion.
     *
     * @param retentionDays the retention period in days to set
     */
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }

    /**
     * Indicates whether this retention rule is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public boolean isEnabled() { return enabled; }

    /**
     * Sets whether this retention rule is enabled.
     *
     * @param enabled the enabled flag to set
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Gets the timestamp of the most recent cleanup run.
     *
     * @return the last run time, or {@code null} if never run
     */
    public ZonedDateTime getLastRunAt() { return lastRunAt; }

    /**
     * Sets the timestamp of the most recent cleanup run.
     *
     * @param lastRunAt the last run time to set
     */
    public void setLastRunAt(ZonedDateTime lastRunAt) { this.lastRunAt = lastRunAt; }

    /**
     * Gets the number of records deleted during the most recent cleanup run.
     *
     * @return the deleted-record count, or {@code null} if never run
     */
    public Integer getLastRunDeletedCount() { return lastRunDeletedCount; }

    /**
     * Sets the number of records deleted during the most recent cleanup run.
     *
     * @param lastRunDeletedCount the deleted-record count to set
     */
    public void setLastRunDeletedCount(Integer lastRunDeletedCount) { this.lastRunDeletedCount = lastRunDeletedCount; }

    /**
     * Gets the timestamp when this rule was created.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedAt() { return createdAt; }

    /**
     * Sets the timestamp when this rule was created.
     *
     * @param createdAt the creation date and time to set
     */
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * Gets the timestamp when this rule was last updated.
     *
     * @return the last update date and time
     */
    public ZonedDateTime getUpdatedAt() { return updatedAt; }

    /**
     * Sets the timestamp when this rule was last updated.
     *
     * @param updatedAt the last update date and time to set
     */
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
