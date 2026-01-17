package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing a status application (service or platform) being monitored.
 * <p>
 * StatusApp is the core entity for tracking the availability and health of services
 * within the status monitoring system. Each application can have multiple components,
 * incidents, and maintenance windows associated with it.
 * </p>
 * <p>
 * The entity includes comprehensive health check configuration allowing automated
 * monitoring of service availability through HTTP/HTTPS endpoints with configurable
 * intervals, timeouts, and failure thresholds.
 * </p>
 *
 * @see StatusComponent
 * @see StatusIncident
 * @see StatusMaintenance
 * @see Tenant
 * @see Organization
 */
@Entity
@Table(name = "status_apps")
public class StatusApp {

    /**
     * Unique identifier for the status application.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Display name of the application or service being monitored.
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Detailed description of the application and its purpose.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * URL-friendly slug identifier for the application.
     * Used in public status page URLs.
     */
    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    /**
     * Flag indicating whether the application's status is publicly visible.
     * Defaults to true for public status pages.
     */
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    /**
     * Current operational status of the application.
     * Defaults to "OPERATIONAL". Common values include OPERATIONAL, DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE.
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status = "OPERATIONAL";

    /**
     * The tenant that owns this application.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    /**
     * The organization that owns this application.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /**
     * The parent platform that this application belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id")
    private StatusPlatform platform;

    /**
     * Flag indicating whether automated health checks are enabled for this application.
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
     * Number of consecutive failures required before marking the application as down.
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
     * API key for authenticating event logging requests.
     * Auto-generated on create or when empty.
     */
    @Column(name = "api_key", length = 64)
    private String apiKey;

    /**
     * Username or identifier of the user who created this application.
     */
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    /**
     * Timestamp indicating when the application was created.
     */
    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    /**
     * Username or identifier of the user who last modified this application.
     */
    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    /**
     * Timestamp indicating when the application was last modified.
     */
    @Column(name = "last_modified_date", nullable = false)
    private ZonedDateTime lastModifiedDate;

    /**
     * Technical timestamp in epoch milliseconds for when the application was created.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * Technical timestamp in epoch milliseconds for when the application was last modified.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "last_modified_date_technical", nullable = false)
    private Long lastModifiedDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new application.
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
     * JPA lifecycle callback executed before updating an existing application.
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
    public StatusApp() {
    }

    /**
     * Gets the unique identifier of the application.
     *
     * @return the UUID of the application
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the application.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the display name of the application.
     *
     * @return the application name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of the application.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of the application.
     *
     * @return the application description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the application.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the URL-friendly slug of the application.
     *
     * @return the slug
     */
    public String getSlug() {
        return slug;
    }

    /**
     * Sets the URL-friendly slug of the application.
     *
     * @param slug the slug to set
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * Checks if the application's status is publicly visible.
     *
     * @return true if public, false otherwise
     */
    public Boolean getIsPublic() {
        return isPublic;
    }

    /**
     * Sets whether the application's status is publicly visible.
     *
     * @param isPublic the public visibility flag to set
     */
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * Gets the current operational status of the application.
     *
     * @return the status (e.g., OPERATIONAL, DEGRADED, MAJOR_OUTAGE)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current operational status of the application.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the tenant that owns this application.
     *
     * @return the parent tenant
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Sets the tenant that owns this application.
     *
     * @param tenant the parent tenant to set
     */
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    /**
     * Gets the organization that owns this application.
     *
     * @return the parent organization
     */
    public Organization getOrganization() {
        return organization;
    }

    /**
     * Sets the organization that owns this application.
     *
     * @param organization the parent organization to set
     */
    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    /**
     * Gets the parent platform that this application belongs to.
     *
     * @return the parent platform
     */
    public StatusPlatform getPlatform() {
        return platform;
    }

    /**
     * Sets the parent platform that this application belongs to.
     *
     * @param platform the parent platform to set
     */
    public void setPlatform(StatusPlatform platform) {
        this.platform = platform;
    }

    /**
     * Gets the username of the user who created this application.
     *
     * @return the creator's username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username of the user who created this application.
     *
     * @param createdBy the creator's username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the creation timestamp of the application.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation timestamp of the application.
     *
     * @param createdDate the creation date and time to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the username of the user who last modified this application.
     *
     * @return the last modifier's username
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the username of the user who last modified this application.
     *
     * @param lastModifiedBy the last modifier's username to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Gets the last modification timestamp of the application.
     *
     * @return the last modification date and time
     */
    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modification timestamp of the application.
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
     * Checks if automated health checks are enabled for this application.
     *
     * @return true if health checks are enabled, false otherwise
     */
    public Boolean getCheckEnabled() {
        return checkEnabled;
    }

    /**
     * Sets whether automated health checks are enabled for this application.
     *
     * @param checkEnabled the enabled flag to set
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
     * Gets the URL endpoint for health checks.
     *
     * @return the check URL
     */
    public String getCheckUrl() {
        return checkUrl;
    }

    /**
     * Sets the URL endpoint for health checks.
     *
     * @param checkUrl the check URL to set
     */
    public void setCheckUrl(String checkUrl) {
        this.checkUrl = checkUrl;
    }

    /**
     * Gets the interval between health check executions in seconds.
     *
     * @return the check interval in seconds
     */
    public Integer getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    /**
     * Sets the interval between health check executions in seconds.
     *
     * @param checkIntervalSeconds the check interval to set
     */
    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    /**
     * Gets the timeout for health check requests in seconds.
     *
     * @return the check timeout in seconds
     */
    public Integer getCheckTimeoutSeconds() {
        return checkTimeoutSeconds;
    }

    /**
     * Sets the timeout for health check requests in seconds.
     *
     * @param checkTimeoutSeconds the check timeout to set
     */
    public void setCheckTimeoutSeconds(Integer checkTimeoutSeconds) {
        this.checkTimeoutSeconds = checkTimeoutSeconds;
    }

    /**
     * Gets the expected HTTP status code for successful health checks.
     *
     * @return the expected status code
     */
    public Integer getCheckExpectedStatus() {
        return checkExpectedStatus;
    }

    /**
     * Sets the expected HTTP status code for successful health checks.
     *
     * @param checkExpectedStatus the expected status code to set
     */
    public void setCheckExpectedStatus(Integer checkExpectedStatus) {
        this.checkExpectedStatus = checkExpectedStatus;
    }

    /**
     * Gets the failure threshold for consecutive check failures.
     *
     * @return the number of consecutive failures before marking as down
     */
    public Integer getCheckFailureThreshold() {
        return checkFailureThreshold;
    }

    /**
     * Sets the failure threshold for consecutive check failures.
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
     * Checks if the last health check was successful.
     *
     * @return true if the last check succeeded, false otherwise
     */
    public Boolean getLastCheckSuccess() {
        return lastCheckSuccess;
    }

    /**
     * Sets whether the last health check was successful.
     *
     * @param lastCheckSuccess the success flag to set
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

    /**
     * Gets the API key for event logging authentication.
     *
     * @return the API key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the API key for event logging authentication.
     *
     * @param apiKey the API key to set
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
