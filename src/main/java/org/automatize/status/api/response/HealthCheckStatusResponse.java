package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Response class for health check status of an entity (app or component).
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Expose the current health/uptime state of a monitored app or component</li>
 *   <li>Carry the configured health-check parameters (type, URL, interval, thresholds)</li>
 *   <li>Report the outcome of the most recent check and consecutive failure count</li>
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
public class HealthCheckStatusResponse {

    /** The unique identifier of the monitored entity. */
    private UUID entityId;

    /** The type of entity being monitored (e.g. app or component). */
    private String entityType;

    /** The display name of the monitored entity. */
    private String name;

    /** The identifier of the platform the entity belongs to. */
    private String platformId;

    /** The display name of the platform the entity belongs to. */
    private String platformName;

    /** Whether health checking is enabled for this entity. */
    private Boolean checkEnabled;

    /** The type of health check performed (e.g. HTTP, TCP). */
    private String checkType;

    /** The URL or target probed by the health check. */
    private String checkUrl;

    /** The interval between health checks, in seconds. */
    private Integer checkIntervalSeconds;

    /** The timeout for each health check, in seconds. */
    private Integer checkTimeoutSeconds;

    /** The HTTP status code expected from a successful check. */
    private Integer checkExpectedStatus;

    /** The number of consecutive failures before the entity is marked down. */
    private Integer checkFailureThreshold;

    /** When the last health check was performed. */
    private ZonedDateTime lastCheckAt;

    /** Whether the last health check succeeded. */
    private Boolean lastCheckSuccess;

    /** The message returned by the last health check. */
    private String lastCheckMessage;

    /** The current number of consecutive failed checks. */
    private Integer consecutiveFailures;

    /** The derived overall status of the entity. */
    private String status;

    /**
     * Default constructor.
     */
    public HealthCheckStatusResponse() {
    }

    /**
     * Gets the entity ID.
     *
     * @return the entity ID
     */
    public UUID getEntityId() {
        return entityId;
    }

    /**
     * Sets the entity ID.
     *
     * @param entityId the entity ID to set
     */
    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    /**
     * Gets the entity type.
     *
     * @return the entity type
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * Sets the entity type.
     *
     * @param entityType the entity type to set
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * Gets the entity name.
     *
     * @return the entity name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the entity name.
     *
     * @param name the entity name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the platform ID.
     *
     * @return the platform ID
     */
    public String getPlatformId() {
        return platformId;
    }

    /**
     * Sets the platform ID.
     *
     * @param platformId the platform ID to set
     */
    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    /**
     * Gets the platform name.
     *
     * @return the platform name
     */
    public String getPlatformName() {
        return platformName;
    }

    /**
     * Sets the platform name.
     *
     * @param platformName the platform name to set
     */
    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    /**
     * Gets the check-enabled flag.
     *
     * @return true if health checking is enabled, false otherwise
     */
    public Boolean getCheckEnabled() {
        return checkEnabled;
    }

    /**
     * Sets the check-enabled flag.
     *
     * @param checkEnabled the check-enabled flag to set
     */
    public void setCheckEnabled(Boolean checkEnabled) {
        this.checkEnabled = checkEnabled;
    }

    /**
     * Gets the check type.
     *
     * @return the check type
     */
    public String getCheckType() {
        return checkType;
    }

    /**
     * Sets the check type.
     *
     * @param checkType the check type to set
     */
    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    /**
     * Gets the check URL.
     *
     * @return the check URL
     */
    public String getCheckUrl() {
        return checkUrl;
    }

    /**
     * Sets the check URL.
     *
     * @param checkUrl the check URL to set
     */
    public void setCheckUrl(String checkUrl) {
        this.checkUrl = checkUrl;
    }

    /**
     * Gets the check interval in seconds.
     *
     * @return the check interval in seconds
     */
    public Integer getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    /**
     * Sets the check interval in seconds.
     *
     * @param checkIntervalSeconds the check interval in seconds to set
     */
    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    /**
     * Gets the check timeout in seconds.
     *
     * @return the check timeout in seconds
     */
    public Integer getCheckTimeoutSeconds() {
        return checkTimeoutSeconds;
    }

    /**
     * Sets the check timeout in seconds.
     *
     * @param checkTimeoutSeconds the check timeout in seconds to set
     */
    public void setCheckTimeoutSeconds(Integer checkTimeoutSeconds) {
        this.checkTimeoutSeconds = checkTimeoutSeconds;
    }

    /**
     * Gets the expected HTTP status code.
     *
     * @return the expected HTTP status code
     */
    public Integer getCheckExpectedStatus() {
        return checkExpectedStatus;
    }

    /**
     * Sets the expected HTTP status code.
     *
     * @param checkExpectedStatus the expected HTTP status code to set
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
     * @param checkFailureThreshold the failure threshold to set
     */
    public void setCheckFailureThreshold(Integer checkFailureThreshold) {
        this.checkFailureThreshold = checkFailureThreshold;
    }

    /**
     * Gets the timestamp of the last check.
     *
     * @return the timestamp of the last check
     */
    public ZonedDateTime getLastCheckAt() {
        return lastCheckAt;
    }

    /**
     * Sets the timestamp of the last check.
     *
     * @param lastCheckAt the timestamp of the last check to set
     */
    public void setLastCheckAt(ZonedDateTime lastCheckAt) {
        this.lastCheckAt = lastCheckAt;
    }

    /**
     * Gets the last check success flag.
     *
     * @return true if the last check succeeded, false otherwise
     */
    public Boolean getLastCheckSuccess() {
        return lastCheckSuccess;
    }

    /**
     * Sets the last check success flag.
     *
     * @param lastCheckSuccess the last check success flag to set
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
     * @param lastCheckMessage the last check message to set
     */
    public void setLastCheckMessage(String lastCheckMessage) {
        this.lastCheckMessage = lastCheckMessage;
    }

    /**
     * Gets the consecutive failure count.
     *
     * @return the consecutive failure count
     */
    public Integer getConsecutiveFailures() {
        return consecutiveFailures;
    }

    /**
     * Sets the consecutive failure count.
     *
     * @param consecutiveFailures the consecutive failure count to set
     */
    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    /**
     * Gets the overall status.
     *
     * @return the overall status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the overall status.
     *
     * @param status the overall status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
