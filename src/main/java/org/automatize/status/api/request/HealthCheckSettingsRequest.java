package org.automatize.status.api.request;

/**
 * Request class for updating the global health-check settings of the status-monitoring app.
 *
 * <p>Carries the tunable parameters for the periodic health-check subsystem: the master
 * enable/disable switch, the scheduler polling interval, the size of the worker thread
 * pool, and the default per-check interval and timeout applied to individual monitored
 * targets. Fields are nullable so that a partial update leaves unspecified settings
 * unchanged.</p>
 */
public class HealthCheckSettingsRequest {

    private Boolean enabled;
    private Long schedulerIntervalMs;
    private Integer threadPoolSize;
    private Integer defaultIntervalSeconds;
    private Integer defaultTimeoutSeconds;

    /**
     * Creates an empty health-check settings request for framework/deserialization use.
     */
    public HealthCheckSettingsRequest() {
    }

    /** @return whether the health-check subsystem is enabled, or {@code null} to leave unchanged */
    public Boolean getEnabled() {
        return enabled;
    }

    /** @param enabled whether the health-check subsystem is enabled */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /** @return the scheduler polling interval in milliseconds, or {@code null} to leave unchanged */
    public Long getSchedulerIntervalMs() {
        return schedulerIntervalMs;
    }

    /** @param schedulerIntervalMs the scheduler polling interval in milliseconds to set */
    public void setSchedulerIntervalMs(Long schedulerIntervalMs) {
        this.schedulerIntervalMs = schedulerIntervalMs;
    }

    /** @return the worker thread-pool size, or {@code null} to leave unchanged */
    public Integer getThreadPoolSize() {
        return threadPoolSize;
    }

    /** @param threadPoolSize the worker thread-pool size to set */
    public void setThreadPoolSize(Integer threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    /** @return the default per-check interval in seconds, or {@code null} to leave unchanged */
    public Integer getDefaultIntervalSeconds() {
        return defaultIntervalSeconds;
    }

    /** @param defaultIntervalSeconds the default per-check interval in seconds to set */
    public void setDefaultIntervalSeconds(Integer defaultIntervalSeconds) {
        this.defaultIntervalSeconds = defaultIntervalSeconds;
    }

    /** @return the default per-check timeout in seconds, or {@code null} to leave unchanged */
    public Integer getDefaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }

    /** @param defaultTimeoutSeconds the default per-check timeout in seconds to set */
    public void setDefaultTimeoutSeconds(Integer defaultTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }
}
