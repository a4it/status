package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

// MED-03: api_key column replaced by key_hash (SHA-256) + key_prefix for display

/**
 * Entity representing an API key used to authenticate log ingestion requests.
 */
@Entity
@Table(name = "log_api_keys")
public class LogApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "key_hash", nullable = false, length = 64, unique = true)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false, length = 8)
    private String keyPrefix;

    /** Transient: populated only in the creation response, never persisted. */
    @Transient
    private String rawKeyOnceOnly;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new API key.
     * Populates the creation timestamps if they have not been set explicitly.
     */
    @PrePersist
    public void prePersist() {
        // Default the human-readable creation timestamp when not already set
        if (createdDate == null) {
            createdDate = ZonedDateTime.now();
        }
        // Default the technical (epoch millis) creation timestamp when not already set
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
    }

    /**
     * Default constructor required by JPA.
     */
    public LogApiKey() {
    }

    /** @return the unique identifier of the API key */
    public UUID getId() { return id; }
    /** @param id the unique identifier to set */
    public void setId(UUID id) { this.id = id; }

    /** @return the tenant that owns this API key */
    public Tenant getTenant() { return tenant; }
    /** @param tenant the owning tenant to set */
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    /** @return the display name of the API key */
    public String getName() { return name; }
    /** @param name the display name to set */
    public void setName(String name) { this.name = name; }

    /** @return the SHA-256 hash of the API key */
    public String getKeyHash() { return keyHash; }
    /** @param keyHash the SHA-256 hash to set */
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    /** @return the short key prefix used for display */
    public String getKeyPrefix() { return keyPrefix; }
    /** @param keyPrefix the display prefix to set */
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    /** @return the raw key value, populated only in the creation response */
    public String getRawKeyOnceOnly() { return rawKeyOnceOnly; }
    /** @param rawKeyOnceOnly the raw key value to set */
    public void setRawKeyOnceOnly(String rawKeyOnceOnly) { this.rawKeyOnceOnly = rawKeyOnceOnly; }

    /** @return true if the key is active, false otherwise */
    public Boolean getIsActive() { return isActive; }
    /** @param isActive the active flag to set */
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    /** @return the creation timestamp */
    public ZonedDateTime getCreatedDate() { return createdDate; }
    /** @param createdDate the creation timestamp to set */
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }

    /** @return the technical creation timestamp in epoch milliseconds */
    public Long getCreatedDateTechnical() { return createdDateTechnical; }
    /** @param createdDateTechnical the technical creation timestamp to set */
    public void setCreatedDateTechnical(Long createdDateTechnical) { this.createdDateTechnical = createdDateTechnical; }
}
