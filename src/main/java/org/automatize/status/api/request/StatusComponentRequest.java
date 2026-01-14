package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request object for creating or updating a status component.
 * <p>
 * Components represent individual parts or services within a status application
 * that can be monitored independently.
 * </p>
 */
public class StatusComponentRequest {

    /** The ID of the parent status application. */
    @NotNull(message = "App ID is required")
    private UUID appId;

    /** The name of the component. */
    @NotBlank(message = "Name is required")
    private String name;

    /** A description of the component. */
    private String description;

    /** The current operational status of the component. */
    private String status = "OPERATIONAL";

    /** The display position/order of the component. */
    private Integer position = 0;

    /** The group name for organizing related components. */
    private String groupName;

    /** Whether to inherit health check settings from the parent application. */
    private Boolean checkInheritFromApp = true;

    /** Whether health checking is enabled for this component. */
    private Boolean checkEnabled = false;

    /** The type of health check to perform. */
    private String checkType = "NONE";

    /** The URL to check for health monitoring. */
    private String checkUrl;

    /** The interval in seconds between health checks. */
    private Integer checkIntervalSeconds = 60;

    /** The timeout in seconds for health check requests. */
    private Integer checkTimeoutSeconds = 10;

    /** The expected HTTP status code for successful health checks. */
    private Integer checkExpectedStatus = 200;

    /** The number of consecutive failures before marking as unhealthy. */
    private Integer checkFailureThreshold = 3;

    /**
     * Default constructor.
     */
    public StatusComponentRequest() {
    }

    /**
     * Gets the parent application ID.
     *
     * @return the app ID
     */
    public UUID getAppId() {
        return appId;
    }

    /**
     * Sets the parent application ID.
     *
     * @param appId the app ID to set
     */
    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    /**
     * Gets the component name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the component name.
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
     * Gets the operational status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the operational status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the display position.
     *
     * @return the position
     */
    public Integer getPosition() {
        return position;
    }

    /**
     * Sets the display position.
     *
     * @param position the position to set
     */
    public void setPosition(Integer position) {
        this.position = position;
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
     * Gets the inherit from app flag.
     *
     * @return true if inheriting settings from app, false otherwise
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
     * Gets the health check interval in seconds.
     *
     * @return the interval in seconds
     */
    public Integer getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    /**
     * Sets the health check interval in seconds.
     *
     * @param checkIntervalSeconds the interval to set
     */
    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    /**
     * Gets the health check timeout in seconds.
     *
     * @return the timeout in seconds
     */
    public Integer getCheckTimeoutSeconds() {
        return checkTimeoutSeconds;
    }

    /**
     * Sets the health check timeout in seconds.
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
}