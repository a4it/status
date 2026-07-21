package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Entity representing a status platform that groups multiple status applications.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Group multiple status applications for hierarchical organization</li>
 *   <li>Maintain platform-level status derived from child applications</li>
 *   <li>Store health check configuration for automated monitoring</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusApp
 * @see Tenant
 * @see Organization
 */
@Entity
@Table(name = "status_platforms")
public class StatusPlatform {

    /**
     * Unique identifier for the platform.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Display name of the platform.
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Detailed description of the platform.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * URL-friendly slug identifier for the platform.
     * Used in public status page URLs.
     */
    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    /**
     * URL to the platform logo image.
     */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /**
     * External website URL for the platform.
     */
    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    /**
     * Current operational status of the platform.
     * Defaults to "OPERATIONAL". Common values include OPERATIONAL, DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE.
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status = "OPERATIONAL";

    /**
     * Flag indicating whether the platform is publicly visible.
     * Defaults to true for public status pages.
     */
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    /**
     * Display position for ordering platforms.
     * Lower values appear first. Defaults to 0.
     */
    @Column(name = "position")
    private Integer position = 0;

    /**
     * The tenant that owns this platform.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    /**
     * The organization that owns this platform.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /**
     * Flag indicating whether automated health checks are enabled for this platform.
     * Defaults to false.
     */
    @Column(name = "check_enabled")
    private Boolean checkEnabled = false;

    /**
     * Type of health check to perform.
     * Defaults to "NONE". Common values include NONE, HTTP, HTTPS, TCP.
     */
    @Column(name = "check_type", length = 50)
    private String checkType = "NONE";

    /**
     * URL endpoint to check for health status.
     * Used when check type is HTTP or HTTPS.
     */
    @Column(name = "check_url", length = 500)
    private String checkUrl;

    /**
     * Interval in seconds between health check executions.
     * Defaults to 60 seconds.
     */
    @Column(name = "check_interval_seconds")
    private Integer checkIntervalSeconds = 60;

    /**
     * Timeout in seconds for health check requests.
     * Defaults to 10 seconds.
     */
    @Column(name = "check_timeout_seconds")
    private Integer checkTimeoutSeconds = 10;

    /**
     * Expected HTTP status code for successful health checks.
     * Defaults to 200.
     */
    @Column(name = "check_expected_status")
    private Integer checkExpectedStatus = 200;

    /**
     * Number of consecutive failures required before marking the platform as down.
     * Defaults to 3 to prevent false positives from transient issues.
     */
    @Column(name = "check_failure_threshold")
    private Integer checkFailureThreshold = 3;

    /**
     * Timestamp of the last health check execution.
     */
    @Column(name = "last_check_at")
    private ZonedDateTime lastCheckAt;

    /**
     * Flag indicating whether the last health check was successful.
     */
    @Column(name = "last_check_success")
    private Boolean lastCheckSuccess;

    /**
     * Message from the last health check providing details about the result.
     */
    @Column(name = "last_check_message", length = 1000)
    private String lastCheckMessage;

    /**
     * Count of consecutive failed health checks.
     * Resets to 0 on successful check.
     */
    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures = 0;

    /**
     * Username or identifier of the user who created this platform.
     */
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    /**
     * Timestamp indicating when the platform was created.
     */
    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    /**
     * Username or identifier of the user who last modified this platform.
     */
    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    /**
     * Timestamp indicating when the platform was last modified.
     */
    @Column(name = "last_modified_date", nullable = false)
    private ZonedDateTime lastModifiedDate;

    /**
     * Technical timestamp in epoch milliseconds for when the platform was created.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * Technical timestamp in epoch milliseconds for when the platform was last modified.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "last_modified_date_technical", nullable = false)
    private Long lastModifiedDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new platform.
     * Automatically sets creation and modification timestamps if not already set.
     */
    @PrePersist
    public void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        // Default the creation timestamp only if it has not been set
        if (createdDate == null) {
            createdDate = now;
        }
        // Default the modification timestamp only if it has not been set
        if (lastModifiedDate == null) {
            lastModifiedDate = now;
        }
        // Default the technical creation timestamp only if it has not been set
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
        // Default the technical modification timestamp only if it has not been set
        if (lastModifiedDateTechnical == null) {
            lastModifiedDateTechnical = System.currentTimeMillis();
        }
    }

    /**
     * JPA lifecycle callback executed before updating an existing platform.
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
    public StatusPlatform() {
    }

    /**
     * Gets the unique identifier of the platform.
     *
     * @return the UUID of the platform
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the platform.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the display name of the platform.
     *
     * @return the platform name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of the platform.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of the platform.
     *
     * @return the platform description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the platform.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the URL-friendly slug identifier of the platform.
     *
     * @return the platform slug
     */
    public String getSlug() {
        return slug;
    }

    /**
     * Sets the URL-friendly slug identifier of the platform.
     *
     * @param slug the slug to set
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * Gets the URL to the platform logo image.
     *
     * @return the logo URL
     */
    public String getLogoUrl() {
        return logoUrl;
    }

    /**
     * Sets the URL to the platform logo image.
     *
     * @param logoUrl the logo URL to set
     */
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    /**
     * Gets the external website URL for the platform.
     *
     * @return the website URL
     */
    public String getWebsiteUrl() {
        return websiteUrl;
    }

    /**
     * Sets the external website URL for the platform.
     *
     * @param websiteUrl the website URL to set
     */
    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    /**
     * Gets the current operational status of the platform.
     *
     * @return the status (e.g., OPERATIONAL, DEGRADED, MAJOR_OUTAGE)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current operational status of the platform.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Checks if the platform is publicly visible.
     *
     * @return true if public, false otherwise
     */
    public Boolean getIsPublic() {
        return isPublic;
    }

    /**
     * Sets whether the platform is publicly visible.
     *
     * @param isPublic the public visibility flag to set
     */
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * Gets the display position used for ordering platforms.
     *
     * @return the display position
     */
    public Integer getPosition() {
        return position;
    }

    /**
     * Sets the display position used for ordering platforms.
     *
     * @param position the display position to set
     */
    public void setPosition(Integer position) {
        this.position = position;
    }

    /**
     * Gets the tenant that owns this platform.
     *
     * @return the owning Tenant
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Sets the tenant that owns this platform.
     *
     * @param tenant the owning Tenant to set
     */
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    /**
     * Gets the organization that owns this platform.
     *
     * @return the owning Organization
     */
    public Organization getOrganization() {
        return organization;
    }

    /**
     * Sets the organization that owns this platform.
     *
     * @param organization the owning Organization to set
     */
    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    /**
     * Gets the username of the user who created this platform.
     *
     * @return the creator's username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username of the user who created this platform.
     *
     * @param createdBy the creator's username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the creation timestamp of the platform.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation timestamp of the platform.
     *
     * @param createdDate the creation date and time to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the username of the user who last modified this platform.
     *
     * @return the last modifier's username
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the username of the user who last modified this platform.
     *
     * @param lastModifiedBy the last modifier's username to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Gets the last modification timestamp of the platform.
     *
     * @return the last modification date and time
     */
    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modification timestamp of the platform.
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

    /**
     * Checks if automated health checks are enabled for this platform.
     *
     * @return true if health checks are enabled, false otherwise
     */
    public Boolean getCheckEnabled() {
        return checkEnabled;
    }

    /**
     * Sets whether automated health checks are enabled for this platform.
     *
     * @param checkEnabled the health check enabled flag to set
     */
    public void setCheckEnabled(Boolean checkEnabled) {
        this.checkEnabled = checkEnabled;
    }

    /**
     * Gets the type of health check to perform.
     *
     * @return the check type (e.g., NONE, HTTP, HTTPS, TCP)
     */
    public String getCheckType() {
        return checkType;
    }

    /**
     * Sets the type of health check to perform.
     *
     * @param checkType the check type to set
     */
    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    /**
     * Gets the URL endpoint checked for health status.
     *
     * @return the check URL
     */
    public String getCheckUrl() {
        return checkUrl;
    }

    /**
     * Sets the URL endpoint checked for health status.
     *
     * @param checkUrl the check URL to set
     */
    public void setCheckUrl(String checkUrl) {
        this.checkUrl = checkUrl;
    }

    /**
     * Gets the interval in seconds between health check executions.
     *
     * @return the check interval in seconds
     */
    public Integer getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    /**
     * Sets the interval in seconds between health check executions.
     *
     * @param checkIntervalSeconds the check interval in seconds to set
     */
    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    /**
     * Gets the timeout in seconds for health check requests.
     *
     * @return the check timeout in seconds
     */
    public Integer getCheckTimeoutSeconds() {
        return checkTimeoutSeconds;
    }

    /**
     * Sets the timeout in seconds for health check requests.
     *
     * @param checkTimeoutSeconds the check timeout in seconds to set
     */
    public void setCheckTimeoutSeconds(Integer checkTimeoutSeconds) {
        this.checkTimeoutSeconds = checkTimeoutSeconds;
    }

    /**
     * Gets the expected HTTP status code for successful health checks.
     *
     * @return the expected HTTP status code
     */
    public Integer getCheckExpectedStatus() {
        return checkExpectedStatus;
    }

    /**
     * Sets the expected HTTP status code for successful health checks.
     *
     * @param checkExpectedStatus the expected HTTP status code to set
     */
    public void setCheckExpectedStatus(Integer checkExpectedStatus) {
        this.checkExpectedStatus = checkExpectedStatus;
    }

    /**
     * Gets the number of consecutive failures required before marking the platform as down.
     *
     * @return the failure threshold
     */
    public Integer getCheckFailureThreshold() {
        return checkFailureThreshold;
    }

    /**
     * Sets the number of consecutive failures required before marking the platform as down.
     *
     * @param checkFailureThreshold the failure threshold to set
     */
    public void setCheckFailureThreshold(Integer checkFailureThreshold) {
        this.checkFailureThreshold = checkFailureThreshold;
    }

    /**
     * Gets the timestamp of the last health check execution.
     *
     * @return the last check timestamp
     */
    public ZonedDateTime getLastCheckAt() {
        return lastCheckAt;
    }

    /**
     * Sets the timestamp of the last health check execution.
     *
     * @param lastCheckAt the last check timestamp to set
     */
    public void setLastCheckAt(ZonedDateTime lastCheckAt) {
        this.lastCheckAt = lastCheckAt;
    }

    /**
     * Checks whether the last health check was successful.
     *
     * @return true if the last check succeeded, false otherwise
     */
    public Boolean getLastCheckSuccess() {
        return lastCheckSuccess;
    }

    /**
     * Sets whether the last health check was successful.
     *
     * @param lastCheckSuccess the last check success flag to set
     */
    public void setLastCheckSuccess(Boolean lastCheckSuccess) {
        this.lastCheckSuccess = lastCheckSuccess;
    }

    /**
     * Gets the message from the last health check.
     *
     * @return the last check message
     */
    public String getLastCheckMessage() {
        return lastCheckMessage;
    }

    /**
     * Sets the message from the last health check.
     *
     * @param lastCheckMessage the last check message to set
     */
    public void setLastCheckMessage(String lastCheckMessage) {
        this.lastCheckMessage = lastCheckMessage;
    }

    /**
     * Gets the count of consecutive failed health checks.
     *
     * @return the consecutive failure count
     */
    public Integer getConsecutiveFailures() {
        return consecutiveFailures;
    }

    /**
     * Sets the count of consecutive failed health checks.
     *
     * @param consecutiveFailures the consecutive failure count to set
     */
    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }
}
