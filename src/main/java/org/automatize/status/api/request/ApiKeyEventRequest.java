package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.ZonedDateTime;

/**
 * <p>
 * Request object for creating a platform event using API key authentication.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate event data for API key-based event submission</li>
 *   <li>Validate required fields such as severity and message</li>
 *   <li>Enable external platforms to log events without JWT authentication</li>
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
public class ApiKeyEventRequest {

    @NotBlank(message = "Severity is required")
    @Pattern(regexp = "^(DEBUG|INFO|WARNING|ERROR|CRITICAL)$", message = "Severity must be DEBUG, INFO, WARNING, ERROR, or CRITICAL")
    private String severity;

    private String source;

    @NotBlank(message = "Message is required")
    private String message;

    private String details;

    private ZonedDateTime eventTime;

    /**
     * Creates an empty API-key event request for framework/deserialization use.
     */
    public ApiKeyEventRequest() {
    }

    /** @return the event severity (DEBUG, INFO, WARNING, ERROR, or CRITICAL) */
    public String getSeverity() {
        return severity;
    }

    /** @param severity the event severity to set */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /** @return the source system or component that produced the event */
    public String getSource() {
        return source;
    }

    /** @param source the event source to set */
    public void setSource(String source) {
        this.source = source;
    }

    /** @return the human-readable event message */
    public String getMessage() {
        return message;
    }

    /** @param message the event message to set */
    public void setMessage(String message) {
        this.message = message;
    }

    /** @return optional additional detail/context for the event */
    public String getDetails() {
        return details;
    }

    /** @param details the additional event detail to set */
    public void setDetails(String details) {
        this.details = details;
    }

    /** @return the timestamp at which the event occurred */
    public ZonedDateTime getEventTime() {
        return eventTime;
    }

    /** @param eventTime the event occurrence timestamp to set */
    public void setEventTime(ZonedDateTime eventTime) {
        this.eventTime = eventTime;
    }
}
