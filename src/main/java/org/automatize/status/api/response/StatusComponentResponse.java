package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Response object containing status component details.
 * <p>
 * Provides information about a component including its status,
 * position, group, and health check configuration and results.
 * </p>
 */
public class StatusComponentResponse {

    /** The unique identifier of the component. */
    private UUID id;

    /** The ID of the parent application. */
    private UUID appId;

    /** The name of the component. */
    private String name;

    /** The description of the component. */
    private String description;

    /** The current operational status. */
    private String status;

    /** The group name for organizing components. */
    private String groupName;

    /** The display position/order. */
    private Integer position;

    /** Whether to inherit health check settings from parent app. */
    private Boolean checkInheritFromApp;

    /** Whether health checking is enabled. */
    private Boolean checkEnabled;

    /** The type of health check performed. */
    private String checkType;

    /** The URL being checked for health monitoring. */
    private String checkUrl;

    /** The interval between health checks in seconds. */
    private Integer checkIntervalSeconds;

    /** The timeout for health check requests in seconds. */
    private Integer checkTimeoutSeconds;

    /** The expected HTTP status code for successful checks. */
    private Integer checkExpectedStatus;

    /** Number of failures before marking unhealthy. */
    private Integer checkFailureThreshold;

    /** When the last health check was performed. */
    private ZonedDateTime lastCheckAt;

    /** Whether the last health check was successful. */
    private Boolean lastCheckSuccess;

    /** The message from the last health check. */
    private String lastCheckMessage;

    /** The current count of consecutive failures. */
    private Integer consecutiveFailures;

    /**
     * Default constructor.
     */
    public StatusComponentResponse() {
    }

    /**
     * Gets the app ID.
     *
     * @return the app ID
     */
    public UUID getAppId() {
        return appId;
    }

    /**
     * Sets the app ID.
     *
     * @param appId the app ID to set
     */
    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    /**
     * Gets the ID.
     *
     * @return the ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the ID.
     *
     * @param id the ID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the group name.
     *
     * @return the group name
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Sets the group name.
     *
     * @param groupName the group name to set
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /**
     * Gets the position.
     *
     * @return the position
     */
    public Integer getPosition() {
        return position;
    }

    /**
     * Sets the position.
     *
     * @param position the position to set
     */
    public void setPosition(Integer position) {
        this.position = position;
    }

    /**
     * Gets the inherit from app flag.
     *
     * @return true if inheriting from app, false otherwise
     */
    public Boolean getCheckInheritFromApp() {
        return checkInheritFromApp;
    }

    /**
     * Sets the inherit from app flag.
     *
     * @param checkInheritFromApp the flag to set
     */
    public void setCheckInheritFromApp(Boolean checkInheritFromApp) {
        this.checkInheritFromApp = checkInheritFromApp;
    }

    /**
     * Gets the health check enabled flag.
     *
     * @return true if enabled, false otherwise
     */
    public Boolean getCheckEnabled() {
        return checkEnabled;
    }

    /**
     * Sets the health check enabled flag.
     *
     * @param checkEnabled the flag to set
     */
    public void setCheckEnabled(Boolean checkEnabled) {
        this.checkEnabled = checkEnabled;
    }

    /**
     * Gets the health check type.
     *
     * @return the check type
     */
    public String getCheckType() {
        return checkType;
    }

    /**
     * Sets the health check type.
     *
     * @param checkType the check type to set
     */
    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    /**
     * Gets the health check URL.
     *
     * @return the check URL
     */
    public String getCheckUrl() {
        return checkUrl;
    }

    /**
     * Sets the health check URL.
     *
     * @param checkUrl the check URL to set
     */
    public void setCheckUrl(String checkUrl) {
        this.checkUrl = checkUrl;
    }

    /**
     * Gets the check interval in seconds.
     *
     * @return the interval in seconds
     */
    public Integer getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    /**
     * Sets the check interval in seconds.
     *
     * @param checkIntervalSeconds the interval to set
     */
    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    /**
     * Gets the check timeout in seconds.
     *
     * @return the timeout in seconds
     */
    public Integer getCheckTimeoutSeconds() {
        return checkTimeoutSeconds;
    }

    /**
     * Sets the check timeout in seconds.
     *
     * @param checkTimeoutSeconds the timeout to set
     */
    public void setCheckTimeoutSeconds(Integer checkTimeoutSeconds) {
        this.checkTimeoutSeconds = checkTimeoutSeconds;
    }

    /**
     * Gets the expected HTTP status code.
     *
     * @return the expected status code
     */
    public Integer getCheckExpectedStatus() {
        return checkExpectedStatus;
    }

    /**
     * Sets the expected HTTP status code.
     *
     * @param checkExpectedStatus the expected status to set
     */
    public void setCheckExpectedStatus(Integer checkExpectedStatus) {
        this.checkExpectedStatus = checkExpectedStatus;
    }

    /**
     * Gets the failure threshold.
     *
     * @return the failure threshold
     */
    public Integer getCheckFailureThreshold() {
        return checkFailureThreshold;
    }

    /**
     * Sets the failure threshold.
     *
     * @param checkFailureThreshold the threshold to set
     */
    public void setCheckFailureThreshold(Integer checkFailureThreshold) {
        this.checkFailureThreshold = checkFailureThreshold;
    }

    /**
     * Gets the last check time.
     *
     * @return the last check time
     */
    public ZonedDateTime getLastCheckAt() {
        return lastCheckAt;
    }

    /**
     * Sets the last check time.
     *
     * @param lastCheckAt the last check time to set
     */
    public void setLastCheckAt(ZonedDateTime lastCheckAt) {
        this.lastCheckAt = lastCheckAt;
    }

    /**
     * Gets the last check success flag.
     *
     * @return true if last check was successful, false otherwise
     */
    public Boolean getLastCheckSuccess() {
        return lastCheckSuccess;
    }

    /**
     * Sets the last check success flag.
     *
     * @param lastCheckSuccess the flag to set
     */
    public void setLastCheckSuccess(Boolean lastCheckSuccess) {
        this.lastCheckSuccess = lastCheckSuccess;
    }

    /**
     * Gets the last check message.
     *
     * @return the last check message
     */
    public String getLastCheckMessage() {
        return lastCheckMessage;
    }

    /**
     * Sets the last check message.
     *
     * @param lastCheckMessage the message to set
     */
    public void setLastCheckMessage(String lastCheckMessage) {
        this.lastCheckMessage = lastCheckMessage;
    }

    /**
     * Gets the consecutive failures count.
     *
     * @return the consecutive failures count
     */
    public Integer getConsecutiveFailures() {
        return consecutiveFailures;
    }

    /**
     * Sets the consecutive failures count.
     *
     * @param consecutiveFailures the count to set
     */
    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }
}