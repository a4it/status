package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response object containing incident details.
 * <p>
 * Provides comprehensive information about an incident including its status,
 * severity, timeline, updates, and affected components.
 * </p>
 */
public class StatusIncidentResponse {

    /** The unique identifier of the incident. */
    private UUID id;

    /** The ID of the parent application. */
    private UUID appId;

    /** The title/summary of the incident. */
    private String title;

    /** The detailed description of the incident. */
    private String description;

    /** The current status of the incident. */
    private String status;

    /** The severity level of the incident. */
    private String severity;

    /** The impact description. */
    private String impact;

    /** When the incident started. */
    private ZonedDateTime startedAt;

    /** When the incident was resolved (null if ongoing). */
    private ZonedDateTime resolvedAt;

    /** Whether this incident is publicly visible. */
    private Boolean isPublic;

    /** List of status updates for this incident. */
    private List<StatusIncidentUpdateResponse> updates;

    /** List of components affected by this incident. */
    private List<StatusComponentResponse> affectedComponents;

    /**
     * Default constructor.
     */
    public StatusIncidentResponse() {
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
     * Gets the title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
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
     * Gets the severity.
     *
     * @return the severity
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Sets the severity.
     *
     * @param severity the severity to set
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /**
     * Gets the impact.
     *
     * @return the impact
     */
    public String getImpact() {
        return impact;
    }

    /**
     * Sets the impact.
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
     * Gets the list of updates.
     *
     * @return the list of updates
     */
    public List<StatusIncidentUpdateResponse> getUpdates() {
        return updates;
    }

    /**
     * Sets the list of updates.
     *
     * @param updates the list of updates to set
     */
    public void setUpdates(List<StatusIncidentUpdateResponse> updates) {
        this.updates = updates;
    }

    /**
     * Gets the list of affected components.
     *
     * @return the list of affected components
     */
    public List<StatusComponentResponse> getAffectedComponents() {
        return affectedComponents;
    }

    /**
     * Sets the list of affected components.
     *
     * @param affectedComponents the list to set
     */
    public void setAffectedComponents(List<StatusComponentResponse> affectedComponents) {
        this.affectedComponents = affectedComponents;
    }
}