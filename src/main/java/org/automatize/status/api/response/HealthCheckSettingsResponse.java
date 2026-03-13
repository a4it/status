package org.automatize.status.api.response;

/**
 * Response class for health check global settings.
 */
public class HealthCheckSettingsResponse {

    private Boolean enabled;
    private Long schedulerIntervalMs;
    private Integer threadPoolSize;
    private Integer defaultIntervalSeconds;
    private Integer defaultTimeoutSeconds;

    public HealthCheckSettingsResponse() {
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getSchedulerIntervalMs() {
        return schedulerIntervalMs;
    }

    public void setSchedulerIntervalMs(Long schedulerIntervalMs) {
        this.schedulerIntervalMs = schedulerIntervalMs;
    }

    public Integer getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(Integer threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public Integer getDefaultIntervalSeconds() {
        return defaultIntervalSeconds;
    }

    public void setDefaultIntervalSeconds(Integer defaultIntervalSeconds) {
        this.defaultIntervalSeconds = defaultIntervalSeconds;
    }

    public Integer getDefaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }

    public void setDefaultTimeoutSeconds(Integer defaultTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }
}
