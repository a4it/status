package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * Request object for creating or updating a status incident.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate incident data for create and update operations</li>
 *   <li>Validate required fields such as app ID, title, status, and severity</li>
 *   <li>Track affected components and incident timeline</li>
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
public class StatusIncidentRequest {

    /** The ID of the status application this incident belongs to. */
    @NotNull(message = "App ID is required")
    private UUID appId;

    /** The title/summary of the incident. */
    @NotBlank(message = "Title is required")
    private String title;

    /** A detailed description of the incident. */
    private String description;

    /** The current status of the incident (e.g., INVESTIGATING, IDENTIFIED, MONITORING, RESOLVED). */
    @NotBlank(message = "Status is required")
    private String status;

    /** The severity level of the incident (e.g., MINOR, MAJOR, CRITICAL). */
    @NotBlank(message = "Severity is required")
    private String severity;

    /** The impact description of the incident. */
    private String impact;

    /** When the incident started. */
    @NotNull(message = "Started at time is required")
    private ZonedDateTime startedAt;

    /** When the incident was resolved (null if ongoing). */
    private ZonedDateTime resolvedAt;

    /** Whether this incident is publicly visible. */
    private Boolean isPublic = true;

    /** List of component IDs affected by this incident. */
    private List<UUID> affectedComponentIds;

    /** The initial status update message for the incident. */
    private String initialMessage;

    /**
     * Default constructor.
     */
    public StatusIncidentRequest() {
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
     * Gets the incident title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the incident title.
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
     * Gets the incident status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the incident status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the severity level.
     *
     * @return the severity
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Sets the severity level.
     *
     * @param severity the severity to set
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /**
     * Gets the impact description.
     *
     * @return the impact
     */
    public String getImpact() {
        return impact;
    }

    /**
     * Sets the impact description.
     *
     * @param impact the impact to set
     */
    public void setImpact(String impact) {
        this.impact = impact;
    }

    /**
     * Gets the start time.
     *
     * @return the start time
     */
    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    /**
     * Sets the start time.
     *
     * @param startedAt the start time to set
     */
    public void setStartedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * Gets the resolution time.
     *
     * @return the resolution time, or null if not resolved
     */
    public ZonedDateTime getResolvedAt() {
        return resolvedAt;
    }

    /**
     * Sets the resolution time.
     *
     * @param resolvedAt the resolution time to set
     */
    public void setResolvedAt(ZonedDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
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

    /**
     * Gets the initial message.
     *
     * @return the initial message
     */
    public String getInitialMessage() {
        return initialMessage;
    }

    /**
     * Sets the initial message.
     *
     * @param initialMessage the initial message to set
     */
    public void setInitialMessage(String initialMessage) {
        this.initialMessage = initialMessage;
    }
}