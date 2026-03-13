package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.ZonedDateTime;
import java.util.UUID;

public class LogRequest {

    private UUID tenantId;

    private ZonedDateTime timestamp;

    @NotBlank(message = "Level is required")
    @Pattern(regexp = "^(DEBUG|INFO|WARNING|ERROR|CRITICAL)$", message = "Level must be DEBUG, INFO, WARNING, ERROR, or CRITICAL")
    private String level;

    @NotBlank(message = "Service is required")
    private String service;

    @NotBlank(message = "Message is required")
    private String message;

    private String metadata;

    private String traceId;

    private String requestId;

    public LogRequest() {
    }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public ZonedDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; }

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
