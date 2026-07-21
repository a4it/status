package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Response object describing a process-mining data retention policy for a
 * tenant and platform within the status-monitoring application.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Identify the tenant and platform the retention policy applies to</li>
 *   <li>Expose the configured retention window and enabled state</li>
 *   <li>Report the outcome of the most recent retention cleanup run</li>
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
public class ProcessMiningRetentionResponse {

    private UUID id;
    private UUID tenantId;
    private String tenantName;
    private UUID platformId;
    private String platformName;
    private int retentionDays;
    private boolean enabled;
    private ZonedDateTime lastRunAt;
    private Integer lastRunDeletedCount;
    private ZonedDateTime createdAt;

    /**
     * Default constructor.
     */
    public ProcessMiningRetentionResponse() {
    }

    /** Gets the retention policy ID. @return the policy ID */
    public UUID getId() { return id; }
    /** Sets the retention policy ID. @param id the policy ID to set */
    public void setId(UUID id) { this.id = id; }

    /** Gets the tenant ID. @return the tenant ID */
    public UUID getTenantId() { return tenantId; }
    /** Sets the tenant ID. @param tenantId the tenant ID to set */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    /** Gets the tenant name. @return the tenant name */
    public String getTenantName() { return tenantName; }
    /** Sets the tenant name. @param tenantName the tenant name to set */
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    /** Gets the platform ID. @return the platform ID */
    public UUID getPlatformId() { return platformId; }
    /** Sets the platform ID. @param platformId the platform ID to set */
    public void setPlatformId(UUID platformId) { this.platformId = platformId; }

    /** Gets the platform name. @return the platform name */
    public String getPlatformName() { return platformName; }
    /** Sets the platform name. @param platformName the platform name to set */
    public void setPlatformName(String platformName) { this.platformName = platformName; }

    /** Gets the retention window in days. @return the retention days */
    public int getRetentionDays() { return retentionDays; }
    /** Sets the retention window in days. @param retentionDays the retention days to set */
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }

    /** Gets whether the retention policy is enabled. @return true if enabled */
    public boolean isEnabled() { return enabled; }
    /** Sets whether the retention policy is enabled. @param enabled the enabled flag to set */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** Gets the timestamp of the last retention run. @return the last run timestamp */
    public ZonedDateTime getLastRunAt() { return lastRunAt; }
    /** Sets the timestamp of the last retention run. @param lastRunAt the last run timestamp to set */
    public void setLastRunAt(ZonedDateTime lastRunAt) { this.lastRunAt = lastRunAt; }

    /** Gets the number of records deleted in the last run. @return the deleted count */
    public Integer getLastRunDeletedCount() { return lastRunDeletedCount; }
    /** Sets the number of records deleted in the last run. @param lastRunDeletedCount the deleted count to set */
    public void setLastRunDeletedCount(Integer lastRunDeletedCount) { this.lastRunDeletedCount = lastRunDeletedCount; }

    /** Gets the creation timestamp of the policy. @return the creation timestamp */
    public ZonedDateTime getCreatedAt() { return createdAt; }
    /** Sets the creation timestamp of the policy. @param createdAt the creation timestamp to set */
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
