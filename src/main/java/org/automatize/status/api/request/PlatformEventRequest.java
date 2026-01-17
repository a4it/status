package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Request object for creating a platform event.
 * <p>
 * Platform events are log entries from monitored platforms and their components.
 * </p>
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
