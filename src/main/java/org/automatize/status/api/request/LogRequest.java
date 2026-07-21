package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Request object for ingesting a structured log entry into the status-monitoring platform.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate log data submitted by monitored services and platforms</li>
 *   <li>Validate the log level against the supported severity vocabulary</li>
 *   <li>Carry tenant scoping and distributed tracing correlation identifiers</li>
 * </ul>
 * </p>
 */
public class LogRequest {

    /** The tenant that owns this log entry, used for multi-tenant scoping. */
    private UUID tenantId;

    /** The timestamp at which the log event occurred. */
    private ZonedDateTime timestamp;

    /** The severity level of the log entry (DEBUG, INFO, WARNING, ERROR, or CRITICAL). */
    @NotBlank(message = "Level is required")
    @Pattern(regexp = "^(DEBUG|INFO|WARNING|ERROR|CRITICAL)$", message = "Level must be DEBUG, INFO, WARNING, ERROR, or CRITICAL")
    private String level;

    /** The name of the service or component that produced the log entry. */
    @NotBlank(message = "Service is required")
    private String service;

    /** The human-readable log message. */
    @NotBlank(message = "Message is required")
    private String message;

    /** Optional additional structured metadata associated with the log entry. */
    private String metadata;

    /** Optional distributed tracing trace identifier for correlation. */
    private String traceId;

    /** Optional request identifier for correlating the log with a specific request. */
    private String requestId;

    /**
     * Default constructor.
     */
    public LogRequest() {
    }

    /**
     * Gets the tenant ID.
     *
     * @return the tenant ID
     */
    public UUID getTenantId() { return tenantId; }

    /**
     * Sets the tenant ID.
     *
     * @param tenantId the tenant ID to set
     */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    /**
     * Gets the event timestamp.
     *
     * @return the timestamp
     */
    public ZonedDateTime getTimestamp() { return timestamp; }

    /**
     * Sets the event timestamp.
     *
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; }

    /**
     * Gets the log level.
     *
     * @return the log level
     */
    public String getLevel() { return level; }

    /**
     * Sets the log level.
     *
     * @param level the log level to set
     */
    public void setLevel(String level) { this.level = level; }

    /**
     * Gets the originating service name.
     *
     * @return the service name
     */
    public String getService() { return service; }

    /**
     * Sets the originating service name.
     *
     * @param service the service name to set
     */
    public void setService(String service) { this.service = service; }

    /**
     * Gets the log message.
     *
     * @return the message
     */
    public String getMessage() { return message; }

    /**
     * Sets the log message.
     *
     * @param message the message to set
     */
    public void setMessage(String message) { this.message = message; }

    /**
     * Gets the additional metadata.
     *
     * @return the metadata
     */
    public String getMetadata() { return metadata; }

    /**
     * Sets the additional metadata.
     *
     * @param metadata the metadata to set
     */
    public void setMetadata(String metadata) { this.metadata = metadata; }

    /**
     * Gets the trace identifier.
     *
     * @return the trace identifier
     */
    public String getTraceId() { return traceId; }

    /**
     * Sets the trace identifier.
     *
     * @param traceId the trace identifier to set
     */
    public void setTraceId(String traceId) { this.traceId = traceId; }

    /**
     * Gets the request identifier.
     *
     * @return the request identifier
     */
    public String getRequestId() { return requestId; }

    /**
     * Sets the request identifier.
     *
     * @param requestId the request identifier to set
     */
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
