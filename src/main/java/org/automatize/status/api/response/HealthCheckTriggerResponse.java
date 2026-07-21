package org.automatize.status.api.response;

import java.time.ZonedDateTime;

/**
 * <p>
 * Response object returned when a health check is manually triggered.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Report whether the triggered health check succeeded</li>
 *   <li>Convey a human-readable result message</li>
 *   <li>Capture the trigger timestamp and execution duration</li>
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
public class HealthCheckTriggerResponse {

    private Boolean success;
    private String message;
    private ZonedDateTime timestamp;
    private Long durationMs;

    /**
     * Default constructor.
     */
    public HealthCheckTriggerResponse() {
    }

    /**
     * Creates a response with the given outcome and message, stamping the
     * current time as the trigger timestamp.
     *
     * @param success whether the health check succeeded
     * @param message the result message
     */
    public HealthCheckTriggerResponse(Boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = ZonedDateTime.now();
    }

    /**
     * Creates a response with the given outcome, message and execution
     * duration, stamping the current time as the trigger timestamp.
     *
     * @param success whether the health check succeeded
     * @param message the result message
     * @param durationMs the execution duration in milliseconds
     */
    public HealthCheckTriggerResponse(Boolean success, String message, Long durationMs) {
        this.success = success;
        this.message = message;
        this.timestamp = ZonedDateTime.now();
        this.durationMs = durationMs;
    }

    /**
     * Gets whether the health check succeeded.
     *
     * @return the success flag
     */
    public Boolean getSuccess() {
        return success;
    }

    /**
     * Sets whether the health check succeeded.
     *
     * @param success the success flag to set
     */
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    /**
     * Gets the result message.
     *
     * @return the result message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the result message.
     *
     * @param message the result message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the trigger timestamp.
     *
     * @return the trigger timestamp
     */
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the trigger timestamp.
     *
     * @param timestamp the trigger timestamp to set
     */
    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the execution duration in milliseconds.
     *
     * @return the execution duration
     */
    public Long getDurationMs() {
        return durationMs;
    }

    /**
     * Sets the execution duration in milliseconds.
     *
     * @param durationMs the execution duration to set
     */
    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}
