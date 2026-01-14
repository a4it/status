package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing an incident affecting a status application.
 * <p>
 * StatusIncident tracks service disruptions, outages, and degradations for monitored
 * applications. Incidents have a lifecycle from creation through investigation,
 * identification, monitoring, and resolution.
 * </p>
 * <p>
 * Each incident can be associated with multiple components (via StatusIncidentComponent)
 * and can have multiple updates (via StatusIncidentUpdate) to communicate progress
 * to users.
 * </p>
 *
 * @see StatusApp
 * @see StatusIncidentComponent
 * @see StatusIncidentUpdate
 */
@Entity
@Table(name = "status_incidents")
public class StatusIncident {

    /**
     * Unique identifier for the incident.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The application affected by this incident.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private StatusApp app;

    /**
     * Brief title or summary of the incident.
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Detailed description of the incident including symptoms and affected functionality.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Current status of the incident in its lifecycle.
     * Common values include INVESTIGATING, IDENTIFIED, MONITORING, RESOLVED.
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /**
     * Severity level of the incident indicating its impact.
     * Common values include CRITICAL, MAJOR, MINOR, MAINTENANCE.
     */
    @Column(name = "severity", nullable = false, length = 50)
    private String severity;

    /**
     * Impact classification describing the breadth of the incident's effect.
     * Common values include NONE, MINOR, MAJOR, CRITICAL.
     */
    @Column(name = "impact", length = 50)
    private String impact;

    /**
     * Timestamp when the incident started or was first detected.
     */
    @Column(name = "started_at", nullable = false)
    private ZonedDateTime startedAt;

    /**
     * Timestamp when the incident was fully resolved.
     * Null while the incident is ongoing.
     */
    @Column(name = "resolved_at")
    private ZonedDateTime resolvedAt;

    /**
     * Flag indicating whether the incident is visible on the public status page.
     * Defaults to true.
     */
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    /**
     * Username or identifier of the user who created this incident.
     */
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    /**
     * Timestamp indicating when the incident record was created.
     */
    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    /**
     * Username or identifier of the user who last modified this incident.
     */
    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    /**
     * Timestamp indicating when the incident was last modified.
     */
    @Column(name = "last_modified_date", nullable = false)
    private ZonedDateTime lastModifiedDate;

    /**
     * Technical timestamp in epoch milliseconds for when the incident was created.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * Technical timestamp in epoch milliseconds for when the incident was last modified.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "last_modified_date_technical", nullable = false)
    private Long lastModifiedDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new incident.
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
     * JPA lifecycle callback executed before updating an existing incident.
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
    public StatusIncident() {
    }

    /**
     * Gets the unique identifier of the incident.
     *
     * @return the UUID of the incident
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the incident.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the application affected by this incident.
     *
     * @return the affected StatusApp
     */
    public StatusApp getApp() {
        return app;
    }

    /**
     * Sets the application affected by this incident.
     *
     * @param app the affected StatusApp to set
     */
    public void setApp(StatusApp app) {
        this.app = app;
    }

    /**
     * Gets the title of the incident.
     *
     * @return the incident title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the incident.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the description of the incident.
     *
     * @return the incident description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the incident.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the current status of the incident.
     *
     * @return the status (e.g., INVESTIGATING, IDENTIFIED, RESOLVED)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current status of the incident.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the severity level of the incident.
     *
     * @return the severity (e.g., CRITICAL, MAJOR, MINOR)
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Sets the severity level of the incident.
     *
     * @param severity the severity to set
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /**
     * Gets the impact classification of the incident.
     *
     * @return the impact level
     */
    public String getImpact() {
        return impact;
    }

    /**
     * Sets the impact classification of the incident.
     *
     * @param impact the impact level to set
     */
    public void setImpact(String impact) {
        this.impact = impact;
    }

    /**
     * Gets the timestamp when the incident started.
     *
     * @return the start timestamp
     */
    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    /**
     * Sets the timestamp when the incident started.
     *
     * @param startedAt the start timestamp to set
     */
    public void setStartedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * Gets the timestamp when the incident was resolved.
     *
     * @return the resolution timestamp, or null if still ongoing
     */
    public ZonedDateTime getResolvedAt() {
        return resolvedAt;
    }

    /**
     * Sets the timestamp when the incident was resolved.
     *
     * @param resolvedAt the resolution timestamp to set
     */
    public void setResolvedAt(ZonedDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    /**
     * Checks if the incident is publicly visible.
     *
     * @return true if public, false otherwise
     */
    public Boolean getIsPublic() {
        return isPublic;
    }

    /**
     * Sets whether the incident is publicly visible.
     *
     * @param isPublic the public visibility flag to set
     */
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * Gets the username of the user who created this incident.
     *
     * @return the creator's username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username of the user who created this incident.
     *
     * @param createdBy the creator's username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the creation timestamp of the incident record.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation timestamp of the incident record.
     *
     * @param createdDate the creation date and time to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the username of the user who last modified this incident.
     *
     * @return the last modifier's username
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the username of the user who last modified this incident.
     *
     * @param lastModifiedBy the last modifier's username to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Gets the last modification timestamp of the incident.
     *
     * @return the last modification date and time
     */
    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modification timestamp of the incident.
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
