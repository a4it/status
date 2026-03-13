package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

public class LogResponse {

    private UUID id;
    private UUID tenantId;
    private ZonedDateTime logTimestamp;
    private String level;
    private String service;
    private String message;
    private String metadata;
    private String traceId;
    private String requestId;

    public LogResponse() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

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
}
