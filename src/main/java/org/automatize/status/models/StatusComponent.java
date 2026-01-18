package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Entity representing a component or sub-service of a status application.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Break down applications into smaller, independently monitored pieces</li>
 *   <li>Support custom or inherited health check configuration</li>
 *   <li>Enable logical grouping for organization on status pages</li>
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
 * @see StatusIncidentComponent
 * @see StatusMaintenanceComponent
 */
@Entity
@Table(name = "status_components")
public class StatusComponent {

    /**
     * Unique identifier for the component.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The parent application that this component belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private StatusApp app;

    /**
     * Display name of the component.
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Detailed description of the component and its function.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Current operational status of the component.
     * Defaults to "OPERATIONAL". Common values include OPERATIONAL, DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE.
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status = "OPERATIONAL";

    /**
     * Display position for ordering components on the status page.
     * Lower values appear first. Defaults to 0.
     */
    @Column(name = "position")
    private Integer position = 0;

    /**
     * Optional group name for organizing related components together.
     */
    @Column(name = "group_name", length = 255)
    private String groupName;

    /**
     * Flag indicating whether to inherit health check configuration from the parent application.
     * Defaults to true.
     */
    @Column(name = "check_inherit_from_app")
    private Boolean checkInheritFromApp = true;

    /**
     * Flag indicating whether automated health checks are enabled for this component.
     * Only applies when checkInheritFromApp is false. Defaults to false.
     */
    @Column(name = "check_enabled")
    private Boolean checkEnabled = false;

    /**
     * Type of health check to perform for this component.
     * Defaults to "NONE". Common values include NONE, HTTP, HTTPS, TCP.
     */
    @Column(name = "check_type", length = 50)
    private String checkType = "NONE";

    /**
     * URL endpoint to check for health status of this component.
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
     * Number of consecutive failures required before marking the component as down.
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
     * Username or identifier of the user who created this component.
     */
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    /**
     * Timestamp indicating when the component was created.
     */
    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    /**
     * Username or identifier of the user who last modified this component.
     */
    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    /**
     * Timestamp indicating when the component was last modified.
     */
    @Column(name = "last_modified_date", nullable = false)
    private ZonedDateTime lastModifiedDate;

    /**
     * Technical timestamp in epoch milliseconds for when the component was created.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * Technical timestamp in epoch milliseconds for when the component was last modified.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "last_modified_date_technical", nullable = false)
    private Long lastModifiedDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new component.
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
     * JPA lifecycle callback executed before updating an existing component.
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
    public StatusComponent() {
    }

    /**
     * Gets the unique identifier of the component.
     *
     * @return the UUID of the component
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the component.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the parent application of this component.
     *
     * @return the parent StatusApp
     */
    public StatusApp getApp() {
        return app;
    }

    /**
     * Sets the parent application of this component.
     *
     * @param app the parent StatusApp to set
     */
    public void setApp(StatusApp app) {
        this.app = app;
    }

    /**
     * Gets the display name of the component.
     *
     * @return the component name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of the component.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of the component.
     *
     * @return the component description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the component.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the current operational status of the component.
     *
     * @return the status (e.g., OPERATIONAL, DEGRADED, MAJOR_OUTAGE)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current operational status of the component.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the display position of the component.
     *
     * @return the position value
     */
    public Integer getPosition() {
        return position;
    }

    /**
     * Sets the display position of the component.
     *
     * @param position the position value to set
     */
    public void setPosition(Integer position) {
        this.position = position;
    }

    /**
     * Gets the group name for organizing this component.
     *
     * @return the group name
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Sets the group name for organizing this component.
     *
     * @param groupName the group name to set
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /**
     * Gets the username of the user who created this component.
     *
     * @return the creator's username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username of the user who created this component.
     *
     * @param createdBy the creator's username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the creation timestamp of the component.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation timestamp of the component.
     *
     * @param createdDate the creation date and time to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the username of the user who last modified this component.
     *
     * @return the last modifier's username
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the username of the user who last modified this component.
     *
     * @param lastModifiedBy the last modifier's username to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Gets the last modification timestamp of the component.
     *
     * @return the last modification date and time
     */
    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modification timestamp of the component.
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
     * Checks if the component inherits health check configuration from the parent application.
     *
     * @return true if inheriting from parent app, false otherwise
     */
    public Boolean getCheckInheritFromApp() {
        return checkInheritFromApp;
    }

    /**
     * Sets whether the component inherits health check configuration from the parent application.
     *
     * @param checkInheritFromApp the inherit flag to set
     */
    public void setCheckInheritFromApp(Boolean checkInheritFromApp) {
        this.checkInheritFromApp = checkInheritFromApp;
    }

    /**
     * Checks if automated health checks are enabled for this component.
     *
     * @return true if health checks are enabled, false otherwise
     */
    public Boolean getCheckEnabled() {
        return checkEnabled;
    }

    /**
     * Sets whether automated health checks are enabled for this component.
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
