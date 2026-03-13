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

    @PrePersist
    public void prePersist() {
        if (logTimestamp == null) {
            logTimestamp = ZonedDateTime.now();
        }
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
    }

    public Log() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public ZonedDateTime getLogTimestamp() { return logTimestamp; }
    public void setLogTimestamp(ZonedDateTime logTimestamp) { this.logTimestamp = logTimestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Long getCreatedDateTechnical() { return createdDateTechnical; }
    public void setCreatedDateTechnical(Long createdDateTechnical) { this.createdDateTechnical = createdDateTechnical; }
}
