package org.automatize.status.api.response;

import java.util.UUID;

public class ContextResponse {

    private String accessToken;
    private UUID tenantId;
    private String tenantName;
    private UUID organizationId;
    private String organizationName;
    private boolean superadmin;
    private boolean hasSelectedContext;

    public ContextResponse() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public boolean isSuperadmin() {
        return superadmin;
    }

    public void setSuperadmin(boolean superadmin) {
        this.superadmin = superadmin;
    }

    public boolean isHasSelectedContext() {
        return hasSelectedContext;
    }

    public void setHasSelectedContext(boolean hasSelectedContext) {
        this.hasSelectedContext = hasSelectedContext;
    }
}
