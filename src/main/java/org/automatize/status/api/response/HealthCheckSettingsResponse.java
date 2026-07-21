package org.automatize.status.api.response;

/**
 * <p>
 * Response object exposing the global health check configuration used by the
 * status-monitoring application's scheduler.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Report whether automated health checking is enabled</li>
 *   <li>Expose scheduler interval and thread pool sizing</li>
 *   <li>Provide default per-check interval and timeout values</li>
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
public class HealthCheckSettingsResponse {

    private Boolean enabled;
    private Long schedulerIntervalMs;
    private Integer threadPoolSize;
    private Integer defaultIntervalSeconds;
    private Integer defaultTimeoutSeconds;

    /**
     * Default constructor.
     */
    public HealthCheckSettingsResponse() {
    }

    /**
     * Gets whether health checking is enabled.
     *
     * @return the enabled flag
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets whether health checking is enabled.
     *
     * @param enabled the enabled flag to set
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the scheduler interval in milliseconds.
     *
     * @return the scheduler interval
     */
    public Long getSchedulerIntervalMs() {
        return schedulerIntervalMs;
    }

    /**
     * Sets the scheduler interval in milliseconds.
     *
     * @param schedulerIntervalMs the scheduler interval to set
     */
    public void setSchedulerIntervalMs(Long schedulerIntervalMs) {
        this.schedulerIntervalMs = schedulerIntervalMs;
    }

    /**
     * Gets the health check thread pool size.
     *
     * @return the thread pool size
     */
    public Integer getThreadPoolSize() {
        return threadPoolSize;
    }

    /**
     * Sets the health check thread pool size.
     *
     * @param threadPoolSize the thread pool size to set
     */
    public void setThreadPoolSize(Integer threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * Gets the default per-check interval in seconds.
     *
     * @return the default interval in seconds
     */
    public Integer getDefaultIntervalSeconds() {
        return defaultIntervalSeconds;
    }

    /**
     * Sets the default per-check interval in seconds.
     *
     * @param defaultIntervalSeconds the default interval to set
     */
    public void setDefaultIntervalSeconds(Integer defaultIntervalSeconds) {
        this.defaultIntervalSeconds = defaultIntervalSeconds;
    }

    /**
     * Gets the default per-check timeout in seconds.
     *
     * @return the default timeout in seconds
     */
    public Integer getDefaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }

    /**
     * Sets the default per-check timeout in seconds.
     *
     * @param defaultTimeoutSeconds the default timeout to set
     */
    public void setDefaultTimeoutSeconds(Integer defaultTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }
}
