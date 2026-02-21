package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class LogApiKeyRequest {

    private UUID tenantId;

    @NotBlank(message = "Name is required")
    private String name;

    public LogApiKeyRequest() {
    }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
