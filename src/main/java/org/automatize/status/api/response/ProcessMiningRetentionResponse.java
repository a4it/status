package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

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

    public ProcessMiningRetentionResponse() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public UUID getPlatformId() { return platformId; }
    public void setPlatformId(UUID platformId) { this.platformId = platformId; }

    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }

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
}
