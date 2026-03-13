package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * Request object for creating or updating a scheduled maintenance window.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate maintenance window data for create and update operations</li>
 *   <li>Validate required fields such as app ID, title, status, and time range</li>
 *   <li>Track affected components during maintenance periods</li>
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
public class StatusMaintenanceRequest {

    /** The ID of the status application this maintenance belongs to. */
    @NotNull(message = "App ID is required")
    private UUID appId;

    /** The title/summary of the maintenance. */
    @NotBlank(message = "Title is required")
    private String title;

    /** A detailed description of the maintenance work. */
    private String description;

    /** The current status of the maintenance (e.g., SCHEDULED, IN_PROGRESS, COMPLETED). */
    @NotBlank(message = "Status is required")
    private String status;

    /** When the maintenance window begins. */
    @NotNull(message = "Start time is required")
    private ZonedDateTime startsAt;

    /** When the maintenance window is expected to end. */
    @NotNull(message = "End time is required")
    private ZonedDateTime endsAt;

    /** Whether this maintenance is publicly visible. */
    private Boolean isPublic = true;

    /** List of component IDs affected by this maintenance. */
    private List<UUID> affectedComponentIds;

    /**
     * Default constructor.
     */
    public StatusMaintenanceRequest() {
    }

    /**
     * Gets the application ID.
     *
     * @return the app ID
     */
    public UUID getAppId() {
        return appId;
    }

    /**
     * Sets the application ID.
     *
     * @param appId the app ID to set
     */
    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    /**
     * Gets the maintenance title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the maintenance title.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
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
     * Gets the maintenance status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the maintenance status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the start time.
     *
     * @return the start time
     */
    public ZonedDateTime getStartsAt() {
        return startsAt;
    }

    /**
     * Sets the start time.
     *
     * @param startsAt the start time to set
     */
    public void setStartsAt(ZonedDateTime startsAt) {
        this.startsAt = startsAt;
    }

    /**
     * Gets the end time.
     *
     * @return the end time
     */
    public ZonedDateTime getEndsAt() {
        return endsAt;
    }

    /**
     * Sets the end time.
     *
     * @param endsAt the end time to set
     */
    public void setEndsAt(ZonedDateTime endsAt) {
        this.endsAt = endsAt;
    }

    /**
     * Gets the public visibility flag.
     *
     * @return true if public, false otherwise
     */
    public Boolean getIsPublic() {
        return isPublic;
    }

    /**
     * Sets the public visibility flag.
     *
     * @param isPublic the public flag to set
     */
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * Gets the list of affected component IDs.
     *
     * @return the list of affected component IDs
     */
    public List<UUID> getAffectedComponentIds() {
        return affectedComponentIds;
    }

    /**
     * Sets the list of affected component IDs.
     *
     * @param affectedComponentIds the component IDs to set
     */
    public void setAffectedComponentIds(List<UUID> affectedComponentIds) {
        this.affectedComponentIds = affectedComponentIds;
    }
}