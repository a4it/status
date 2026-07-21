package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Request payload for creating a log-ingestion API key in the status-monitoring app.
 *
 * <p>External platforms use the resulting API key to submit logs and events without JWT
 * authentication. The request scopes the key to a tenant and assigns it a descriptive
 * name for identification and management.</p>
 */
public class LogApiKeyRequest {

    private UUID tenantId;

    @NotBlank(message = "Name is required")
    private String name;

    /**
     * Creates an empty log API-key request for framework/deserialization use.
     */
    public LogApiKeyRequest() {
    }

    /** @return the tenant the API key is scoped to */
    public UUID getTenantId() { return tenantId; }
    /** @param tenantId the owning tenant identifier to set */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    /** @return the descriptive name of the API key */
    public String getName() { return name; }
    /** @param name the API key name to set */
    public void setName(String name) { this.name = name; }
}
