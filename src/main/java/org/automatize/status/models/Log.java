package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing a log entry ingested via the Logs Hub REST API.
 */
@Entity
@Table(name = "logs")
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "log_timestamp", nullable = false)
    private ZonedDateTime logTimestamp;

    @Column(name = "level", nullable = false, length = 20)
    private String level;

    @Column(name = "service", nullable = false, length = 255)
    private String service;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "trace_id", length = 255)
    private String traceId;

    @Column(name = "request_id", length = 255)
    private String requestId;

    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new log entry.
     * Populates the log timestamp and technical creation timestamp if not set explicitly.
     */
    @PrePersist
    public void prePersist() {
        // Default the log timestamp to now when not provided by the caller
        if (logTimestamp == null) {
            logTimestamp = ZonedDateTime.now();
        }
        // Default the technical (epoch millis) creation timestamp when not already set
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
    }

    /**
     * Default constructor required by JPA.
     */
    public Log() {
    }

    /** @return the unique identifier of the log entry */
    public UUID getId() { return id; }
    /** @param id the unique identifier to set */
    public void setId(UUID id) { this.id = id; }

    /** @return the tenant that owns this log entry */
    public Tenant getTenant() { return tenant; }
    /** @param tenant the owning tenant to set */
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    /** @return the timestamp of the log event */
    public ZonedDateTime getLogTimestamp() { return logTimestamp; }
    /** @param logTimestamp the log event timestamp to set */
    public void setLogTimestamp(ZonedDateTime logTimestamp) { this.logTimestamp = logTimestamp; }

    /** @return the log level */
    public String getLevel() { return level; }
    /** @param level the log level to set */
    public void setLevel(String level) { this.level = level; }

    /** @return the originating service name */
    public String getService() { return service; }
    /** @param service the originating service name to set */
    public void setService(String service) { this.service = service; }

    /** @return the log message */
    public String getMessage() { return message; }
    /** @param message the log message to set */
    public void setMessage(String message) { this.message = message; }

    /** @return the optional metadata payload */
    public String getMetadata() { return metadata; }
    /** @param metadata the metadata payload to set */
    public void setMetadata(String metadata) { this.metadata = metadata; }

    /** @return the distributed trace identifier */
    public String getTraceId() { return traceId; }
    /** @param traceId the trace identifier to set */
    public void setTraceId(String traceId) { this.traceId = traceId; }

    /** @return the request identifier */
    public String getRequestId() { return requestId; }
    /** @param requestId the request identifier to set */
    public void setRequestId(String requestId) { this.requestId = requestId; }

    /** @return the technical creation timestamp in epoch milliseconds */
    public Long getCreatedDateTechnical() { return createdDateTechnical; }
    /** @param createdDateTechnical the technical creation timestamp to set */
    public void setCreatedDateTechnical(Long createdDateTechnical) { this.createdDateTechnical = createdDateTechnical; }
}
