package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

// MED-03: apiKey field replaced by keyPrefix (always shown) and rawKey (only on creation)
public class LogApiKeyResponse {

    private UUID id;
    private UUID tenantId;
    private String name;
    /** First 8 characters of the key — safe to show in list views. */
    private String keyPrefix;
    /** Full plaintext key — populated only in the creation response, null on all subsequent reads. */
    private String rawKey;
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

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public String getRawKey() { return rawKey; }
    public void setRawKey(String rawKey) { this.rawKey = rawKey; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public ZonedDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }
}
