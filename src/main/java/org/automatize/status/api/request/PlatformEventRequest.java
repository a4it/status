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

    @NotNull(message = "App ID is required")
    private UUID appId;

    private UUID componentId;

    @NotBlank(message = "Severity is required")
    @Pattern(regexp = "^(DEBUG|INFO|WARNING|ERROR|CRITICAL)$", message = "Severity must be DEBUG, INFO, WARNING, ERROR, or CRITICAL")
    private String severity;

    private String source;

    @NotBlank(message = "Message is required")
    private String message;

    private String details;

    private ZonedDateTime eventTime;

    public PlatformEventRequest() {
    }

    public UUID getAppId() {
        return appId;
    }

    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    public UUID getComponentId() {
        return componentId;
    }

    public void setComponentId(UUID componentId) {
        this.componentId = componentId;
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
