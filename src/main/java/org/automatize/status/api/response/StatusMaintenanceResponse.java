package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response object containing scheduled maintenance details.
 * <p>
 * Provides information about a maintenance window including its schedule,
 * status, and affected components.
 * </p>
 */
public class StatusMaintenanceResponse {

    /** The unique identifier of the maintenance. */
    private UUID id;

    /** The title/summary of the maintenance. */
    private String title;

    /** The detailed description of the maintenance. */
    private String description;

    /** The current status of the maintenance. */
    private String status;

    /** When the maintenance is scheduled to start. */
    private ZonedDateTime startsAt;

    /** When the maintenance is expected to end. */
    private ZonedDateTime endsAt;

    /** List of components affected by this maintenance. */
    private List<StatusComponentResponse> affectedComponents;

    /**
     * Default constructor.
     */
    public StatusMaintenanceResponse() {
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