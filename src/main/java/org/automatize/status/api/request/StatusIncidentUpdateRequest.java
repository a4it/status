package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

import java.time.ZonedDateTime;

/**
 * <p>
 * Request object for creating an incident status update.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate incident update data including status and message</li>
 *   <li>Validate required fields such as status and message</li>
 *   <li>Track update timestamp for incident timeline</li>
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
public class StatusIncidentUpdateRequest {

    /** The new status of the incident after this update. */
    @NotBlank(message = "Status is required")
    private String status;

    /** The update message describing the current situation or progress. */
    @NotBlank(message = "Message is required")
    private String message;

    /** The time of the update (defaults to current time if not specified). */
    private ZonedDateTime updateTime;

    /**
     * Default constructor.
     */
    public StatusIncidentUpdateRequest() {
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
     * Gets the message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     *
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the update time.
     *
     * @return the update time
     */
    public ZonedDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * Sets the update time.
     *
     * @param updateTime the update time to set
     */
    public void setUpdateTime(ZonedDateTime updateTime) {
        this.updateTime = updateTime;
    }
}