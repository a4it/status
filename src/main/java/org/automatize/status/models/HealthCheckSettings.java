package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing global health check configuration settings.
 * <p>
 * This entity stores key-value pairs for health check scheduler configuration,
 * allowing runtime changes without application restart.
 * </p>
 */
@Entity
@Table(name = "health_check_settings")
public class HealthCheckSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 500)
    private String settingValue;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private ZonedDateTime lastModifiedDate;

    /**
     * JPA lifecycle callback executed before persisting a new settings row.
     * Populates the creation and last-modified timestamps if they have not been set explicitly.
     */
    @PrePersist
    public void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        // Default the creation timestamp when not already set
        if (createdDate == null) {
            createdDate = now;
        }
        // Default the last-modified timestamp when not already set
        if (lastModifiedDate == null) {
            lastModifiedDate = now;
        }
    }

    /**
     * JPA lifecycle callback executed before updating an existing settings row.
     * Refreshes the last-modified timestamp.
     */
    @PreUpdate
    public void preUpdate() {
        lastModifiedDate = ZonedDateTime.now();
    }

    /**
     * Default constructor required by JPA.
     */
    public HealthCheckSettings() {
    }

    /** @return the unique identifier of the settings row */
    public UUID getId() {
        return id;
    }

    /** @param id the unique identifier to set */
    public void setId(UUID id) {
        this.id = id;
    }

    /** @return the setting key */
    public String getSettingKey() {
        return settingKey;
    }

    /** @param settingKey the setting key to set */
    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    /** @return the setting value */
    public String getSettingValue() {
        return settingValue;
    }

    /** @param settingValue the setting value to set */
    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    /** @return the human-readable description of the setting */
    public String getDescription() {
        return description;
    }

    /** @param description the description to set */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the creation timestamp */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /** @param createdDate the creation timestamp to set */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /** @return the last-modified timestamp */
    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /** @param lastModifiedDate the last-modified timestamp to set */
    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
