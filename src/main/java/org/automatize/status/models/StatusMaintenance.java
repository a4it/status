package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing a scheduled maintenance window for a status application.
 * <p>
 * StatusMaintenance allows operators to announce planned maintenance periods
 * in advance, helping users prepare for expected downtime or degraded service.
 * Maintenance windows have defined start and end times and can be associated
 * with specific components.
 * </p>
 * <p>
 * Unlike incidents which are unplanned, maintenance represents expected and
 * controlled service interruptions for updates, upgrades, or other planned work.
 * </p>
 *
 * @see StatusApp
 * @see StatusMaintenanceComponent
 */
@Entity
@Table(name = "status_maintenance")
public class StatusMaintenance {

    /**
     * Unique identifier for the maintenance window.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The application this maintenance window applies to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private StatusApp app;

    /**
     * Brief title or summary of the maintenance work.
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Detailed description of the maintenance work and expected impact.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Current status of the maintenance window.
     * Common values include SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED.
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /**
     * Scheduled start time for the maintenance window.
     */
    @Column(name = "starts_at", nullable = false)
    private ZonedDateTime startsAt;

    /**
     * Scheduled end time for the maintenance window.
     */
    @Column(name = "ends_at", nullable = false)
    private ZonedDateTime endsAt;

    /**
     * Flag indicating whether the maintenance is visible on the public status page.
     * Defaults to true.
     */
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    /**
     * Username or identifier of the user who created this maintenance window.
     */
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    /**
     * Timestamp indicating when the maintenance record was created.
     */
    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    /**
     * Username or identifier of the user who last modified this maintenance window.
     */
    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    /**
     * Timestamp indicating when the maintenance was last modified.
     */
    @Column(name = "last_modified_date", nullable = false)
    private ZonedDateTime lastModifiedDate;

    /**
     * Technical timestamp in epoch milliseconds for when the maintenance was created.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * Technical timestamp in epoch milliseconds for when the maintenance was last modified.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "last_modified_date_technical", nullable = false)
    private Long lastModifiedDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new maintenance window.
     * Automatically sets creation and modification timestamps if not already set.
     */
    @PrePersist
    public void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        if (createdDate == null) {
            createdDate = now;
        }
        if (lastModifiedDate == null) {
            lastModifiedDate = now;
        }
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
        if (lastModifiedDateTechnical == null) {
            lastModifiedDateTechnical = System.currentTimeMillis();
        }
    }

    /**
     * JPA lifecycle callback executed before updating an existing maintenance window.
     * Automatically updates the modification timestamps.
     */
    @PreUpdate
    public void preUpdate() {
        lastModifiedDate = ZonedDateTime.now();
        lastModifiedDateTechnical = System.currentTimeMillis();
    }

    /**
     * Default constructor required by JPA.
     */
    public StatusMaintenance() {
    }

    /**
     * Gets the unique identifier of the maintenance window.
     *
     * @return the UUID of the maintenance
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the maintenance window.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the application this maintenance window applies to.
     *
     * @return the associated StatusApp
     */
    public StatusApp getApp() {
        return app;
    }

    /**
     * Sets the application this maintenance window applies to.
     *
     * @param app the StatusApp to set
     */
    public void setApp(StatusApp app) {
        this.app = app;
    }

    /**
     * Gets the title of the maintenance window.
     *
     * @return the maintenance title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the maintenance window.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the description of the maintenance work.
     *
     * @return the maintenance description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the maintenance work.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the current status of the maintenance window.
     *
     * @return the status (e.g., SCHEDULED, IN_PROGRESS, COMPLETED)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current status of the maintenance window.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the scheduled start time for the maintenance.
     *
     * @return the start timestamp
     */
    public ZonedDateTime getStartsAt() {
        return startsAt;
    }

    /**
     * Sets the scheduled start time for the maintenance.
     *
     * @param startsAt the start timestamp to set
     */
    public void setStartsAt(ZonedDateTime startsAt) {
        this.startsAt = startsAt;
    }

    /**
     * Gets the scheduled end time for the maintenance.
     *
     * @return the end timestamp
     */
    public ZonedDateTime getEndsAt() {
        return endsAt;
    }

    /**
     * Sets the scheduled end time for the maintenance.
     *
     * @param endsAt the end timestamp to set
     */
    public void setEndsAt(ZonedDateTime endsAt) {
        this.endsAt = endsAt;
    }

    /**
     * Checks if the maintenance is publicly visible.
     *
     * @return true if public, false otherwise
     */
    public Boolean getIsPublic() {
        return isPublic;
    }

    /**
     * Sets whether the maintenance is publicly visible.
     *
     * @param isPublic the public visibility flag to set
     */
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * Gets the username of the user who created this maintenance window.
     *
     * @return the creator's username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username of the user who created this maintenance window.
     *
     * @param createdBy the creator's username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the creation timestamp of the maintenance record.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation timestamp of the maintenance record.
     *
     * @param createdDate the creation date and time to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the username of the user who last modified this maintenance window.
     *
     * @return the last modifier's username
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the username of the user who last modified this maintenance window.
     *
     * @param lastModifiedBy the last modifier's username to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Gets the last modification timestamp of the maintenance.
     *
     * @return the last modification date and time
     */
    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modification timestamp of the maintenance.
     *
     * @param lastModifiedDate the last modification date and time to set
     */
    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Gets the technical creation timestamp in epoch milliseconds.
     *
     * @return the creation timestamp in milliseconds since epoch
     */
    public Long getCreatedDateTechnical() {
        return createdDateTechnical;
    }

    /**
     * Sets the technical creation timestamp in epoch milliseconds.
     *
     * @param createdDateTechnical the creation timestamp in milliseconds to set
     */
    public void setCreatedDateTechnical(Long createdDateTechnical) {
        this.createdDateTechnical = createdDateTechnical;
    }

    /**
     * Gets the technical last modification timestamp in epoch milliseconds.
     *
     * @return the last modification timestamp in milliseconds since epoch
     */
    public Long getLastModifiedDateTechnical() {
        return lastModifiedDateTechnical;
    }

    /**
     * Sets the technical last modification timestamp in epoch milliseconds.
     *
     * @param lastModifiedDateTechnical the last modification timestamp in milliseconds to set
     */
    public void setLastModifiedDateTechnical(Long lastModifiedDateTechnical) {
        this.lastModifiedDateTechnical = lastModifiedDateTechnical;
    }
}
