package org.automatize.status.api.request;

import java.util.UUID;

/**
 * <p>
 * Request object for configuring process-mining data retention for a monitored platform.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate retention settings scoped to a tenant and platform</li>
 *   <li>Specify how many days of process-mining data are retained</li>
 *   <li>Enable or disable the retention policy</li>
 * </ul>
 * </p>
 */
public class ProcessMiningRetentionRequest {

    /** The tenant that owns the retention configuration. */
    private UUID tenantId;

    /** The platform the retention policy applies to. */
    private UUID platformId;

    /** The number of days process-mining data should be retained. */
    private int retentionDays;

    /** Whether the retention policy is enabled. */
    private boolean enabled;

    /**
     * Default constructor.
     */
    public ProcessMiningRetentionRequest() {
    }

    /**
     * Gets the tenant ID.
     *
     * @return the tenant ID
     */
    public UUID getTenantId() { return tenantId; }

    /**
     * Sets the tenant ID.
     *
     * @param tenantId the tenant ID to set
     */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    /**
     * Gets the platform ID.
     *
     * @return the platform ID
     */
    public UUID getPlatformId() { return platformId; }

    /**
     * Sets the platform ID.
     *
     * @param platformId the platform ID to set
     */
    public void setPlatformId(UUID platformId) { this.platformId = platformId; }

    /**
     * Gets the retention period in days.
     *
     * @return the retention days
     */
    public int getRetentionDays() { return retentionDays; }

    /**
     * Sets the retention period in days.
     *
     * @param retentionDays the retention days to set
     */
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }

    /**
     * Gets whether the retention policy is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() { return enabled; }

    /**
     * Sets whether the retention policy is enabled.
     *
     * @param enabled the enabled flag to set
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
