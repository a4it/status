package org.automatize.status.api.response;

import java.time.ZonedDateTime;

/**
 * Response class for health check trigger results.
 */
public class HealthCheckTriggerResponse {

    private Boolean success;
    private String message;
    private ZonedDateTime timestamp;
    private Long durationMs;

    public HealthCheckTriggerResponse() {
    }

    public HealthCheckTriggerResponse(Boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = ZonedDateTime.now();
    }

    public HealthCheckTriggerResponse(Boolean success, String message, Long durationMs) {
        this.success = success;
        this.message = message;
        this.timestamp = ZonedDateTime.now();
        this.durationMs = durationMs;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}
