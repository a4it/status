package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Response object containing log API key details.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Expose the non-sensitive metadata of a log ingestion API key</li>
 *   <li>Always surface the safe {@code keyPrefix} for identification in list views</li>
 *   <li>Return the full {@code rawKey} only once, in the creation response</li>
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
// MED-03: apiKey field replaced by keyPrefix (always shown) and rawKey (only on creation)
public class LogApiKeyResponse {

    /** The unique identifier of the API key. */
    private UUID id;

    /** The identifier of the owning tenant. */
    private UUID tenantId;

    /** The display name of the API key. */
    private String name;
    /** First 8 characters of the key — safe to show in list views. */
    private String keyPrefix;
    /** Full plaintext key — populated only in the creation response, null on all subsequent reads. */
    private String rawKey;

    /** Whether the API key is currently active. */
    private Boolean isActive;

    /** When the API key was created. */
    private ZonedDateTime createdDate;

    /**
     * Default constructor.
     */
    public LogApiKeyResponse() {
    }

    /** Gets the ID. @return the ID */
    public UUID getId() { return id; }
    /** Sets the ID. @param id the ID to set */
    public void setId(UUID id) { this.id = id; }

    /** Gets the tenant ID. @return the tenant ID */
    public UUID getTenantId() { return tenantId; }
    /** Sets the tenant ID. @param tenantId the tenant ID to set */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    /** Gets the name. @return the name */
    public String getName() { return name; }
    /** Sets the name. @param name the name to set */
    public void setName(String name) { this.name = name; }

    /** Gets the key prefix. @return the key prefix */
    public String getKeyPrefix() { return keyPrefix; }
    /** Sets the key prefix. @param keyPrefix the key prefix to set */
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    /** Gets the raw plaintext key. @return the raw key, or null outside the creation response */
    public String getRawKey() { return rawKey; }
    /** Sets the raw plaintext key. @param rawKey the raw key to set */
    public void setRawKey(String rawKey) { this.rawKey = rawKey; }

    /** Gets the active status. @return true if active, false otherwise */
    public Boolean getIsActive() { return isActive; }
    /** Sets the active status. @param isActive the active status to set */
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    /** Gets the creation date. @return the creation date */
    public ZonedDateTime getCreatedDate() { return createdDate; }
    /** Sets the creation date. @param createdDate the creation date to set */
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }
}
