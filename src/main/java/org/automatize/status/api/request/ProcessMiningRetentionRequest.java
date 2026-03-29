package org.automatize.status.api.request;

import java.util.UUID;

public class ProcessMiningRetentionRequest {

    private UUID tenantId;
    private UUID platformId;
    private int retentionDays;
    private boolean enabled;

    public ProcessMiningRetentionRequest() {
    }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getPlatformId() { return platformId; }
    public void setPlatformId(UUID platformId) { this.platformId = platformId; }

    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
