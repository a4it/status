package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Response object representing a single structured application log entry.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Expose a persisted log record for a given tenant to API consumers</li>
 *   <li>Carry severity, originating service and the log message</li>
 *   <li>Provide correlation identifiers (trace and request IDs) for tracing across the monitoring platform</li>
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
public class LogResponse {

    /** The unique identifier of the log entry. */
    private UUID id;

    /** The identifier of the tenant that owns this log entry. */
    private UUID tenantId;

    /** The timestamp when the log entry was recorded. */
    private ZonedDateTime logTimestamp;

    /** The severity level of the log entry (e.g. INFO, WARN, ERROR). */
    private String level;

    /** The name of the service that produced the log entry. */
    private String service;

    /** The log message text. */
    private String message;

    /** Additional structured metadata associated with the log entry. */
    private String metadata;

    /** The distributed trace identifier for correlation. */
    private String traceId;

    /** The request identifier for correlation. */
    private String requestId;

    /**
     * Default constructor.
     */
    public LogResponse() {
    }

    /**
     * Gets the log entry ID.
     *
     * @return the log entry ID
     */
    public UUID getId() { return id; }

    /**
     * Sets the log entry ID.
     *
     * @param id the log entry ID to set
     */
    public void setId(UUID id) { this.id = id; }

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
     * Gets the log timestamp.
     *
     * @return the log timestamp
     */
    public ZonedDateTime getLogTimestamp() { return logTimestamp; }

    /**
     * Sets the log timestamp.
     *
     * @param logTimestamp the log timestamp to set
     */
    public void setLogTimestamp(ZonedDateTime logTimestamp) { this.logTimestamp = logTimestamp; }

    /**
     * Gets the severity level.
     *
     * @return the severity level
     */
    public String getLevel() { return level; }

    /**
     * Sets the severity level.
     *
     * @param level the severity level to set
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
     * @return the log message
     */
    public String getMessage() { return message; }

    /**
     * Sets the log message.
     *
     * @param message the log message to set
     */
    public void setMessage(String message) { this.message = message; }

    /**
     * Gets the metadata.
     *
     * @return the metadata
     */
    public String getMetadata() { return metadata; }

    /**
     * Sets the metadata.
     *
     * @param metadata the metadata to set
     */
    public void setMetadata(String metadata) { this.metadata = metadata; }

    /**
     * Gets the trace ID.
     *
     * @return the trace ID
     */
    public String getTraceId() { return traceId; }

    /**
     * Sets the trace ID.
     *
     * @param traceId the trace ID to set
     */
    public void setTraceId(String traceId) { this.traceId = traceId; }

    /**
     * Gets the request ID.
     *
     * @return the request ID
     */
    public String getRequestId() { return requestId; }

    /**
     * Sets the request ID.
     *
     * @param requestId the request ID to set
     */
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
