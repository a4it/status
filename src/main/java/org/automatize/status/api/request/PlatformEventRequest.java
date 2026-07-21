package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Request object for creating a platform event.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate platform event data for logging operations</li>
 *   <li>Validate required fields such as app ID, severity, and message</li>
 *   <li>Associate events with specific applications and components</li>
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
public class PlatformEventRequest {

    /** The application this event belongs to. */
    @NotNull(message = "App ID is required")
    private UUID appId;

    /** The specific component within the application that the event relates to (optional). */
    private UUID componentId;

    /** The severity of the event (DEBUG, INFO, WARNING, ERROR, or CRITICAL). */
    @NotBlank(message = "Severity is required")
    @Pattern(regexp = "^(DEBUG|INFO|WARNING|ERROR|CRITICAL)$", message = "Severity must be DEBUG, INFO, WARNING, ERROR, or CRITICAL")
    private String severity;

    /** The source that raised the event (optional). */
    private String source;

    /** The human-readable event message. */
    @NotBlank(message = "Message is required")
    private String message;

    /** Optional additional details describing the event. */
    private String details;

    /** The time at which the event occurred. */
    private ZonedDateTime eventTime;

    /**
     * Default constructor.
     */
    public PlatformEventRequest() {
    }

    /**
     * Gets the application ID.
     *
     * @return the application ID
     */
    public UUID getAppId() {
        return appId;
    }

    /**
     * Sets the application ID.
     *
     * @param appId the application ID to set
     */
    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    /**
     * Gets the component ID.
     *
     * @return the component ID
     */
    public UUID getComponentId() {
        return componentId;
    }

    /**
     * Sets the component ID.
     *
     * @param componentId the component ID to set
     */
    public void setComponentId(UUID componentId) {
        this.componentId = componentId;
    }

    /**
     * Gets the event severity.
     *
     * @return the severity
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Sets the event severity.
     *
     * @param severity the severity to set
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /**
     * Gets the event source.
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the event source.
     *
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets the event message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the event message.
     *
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the event details.
     *
     * @return the details
     */
    public String getDetails() {
        return details;
    }

    /**
     * Sets the event details.
     *
     * @param details the details to set
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Gets the event time.
     *
     * @return the event time
     */
    public ZonedDateTime getEventTime() {
        return eventTime;
    }

    /**
     * Sets the event time.
     *
     * @param eventTime the event time to set
     */
    public void setEventTime(ZonedDateTime eventTime) {
        this.eventTime = eventTime;
    }
}
