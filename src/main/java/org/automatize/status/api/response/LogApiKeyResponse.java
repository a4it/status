package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

public class LogApiKeyResponse {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String apiKey;
    private Boolean isActive;
    private ZonedDateTime createdDate;

    public LogApiKeyResponse() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public ZonedDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }
}
