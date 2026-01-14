package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Response object containing incident update details.
 * <p>
 * Represents a single status update posted during an incident's lifecycle,
 * tracking progress from investigation through resolution.
 * </p>
 */
public class StatusIncidentUpdateResponse {

    /** The unique identifier of the update. */
    private UUID id;

    /** The incident status at the time of this update. */
    private String status;

    /** The update message content. */
    private String message;

    /** When this update was posted. */
    private ZonedDateTime updateTime;

    /** The user who created this update. */
    private String createdBy;

    /**
     * Default constructor.
     */
    public StatusIncidentUpdateResponse() {
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

    /**
     * Gets the creator's name.
     *
     * @return the creator's name
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the creator's name.
     *
     * @param createdBy the creator's name to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}