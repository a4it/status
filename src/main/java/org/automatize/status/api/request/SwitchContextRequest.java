package org.automatize.status.api.request;

import java.util.UUID;

public class SwitchContextRequest {

    private UUID tenantId;
    private UUID organizationId;

    public SwitchContextRequest() {
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}
