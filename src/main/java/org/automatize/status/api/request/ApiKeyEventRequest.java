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

    public ApiKeyEventRequest() {
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public ZonedDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(ZonedDateTime eventTime) {
        this.eventTime = eventTime;
    }
}
