package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.ZonedDateTime;

/**
 * Request object for creating a platform event using API key authentication.
 * <p>
 * This request is used by external platforms and components to log events
 * without requiring JWT authentication. The API key identifies the source.
 * </p>
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
