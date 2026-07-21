package org.automatize.status.api.request;

/**
 * Request payload for configuring the log garbage-collection (GC) schedule.
 *
 * <p>Controls whether the background job that purges/cleans up old log data runs and,
 * when enabled, the {@code cron} expression that determines its execution cadence.</p>
 */
public class GcScheduleRequest {

    private boolean enabled;
    private String cron;

    /**
     * Creates an empty GC schedule request for framework/deserialization use.
     */
    public GcScheduleRequest() {
    }

    /** @return {@code true} if the garbage-collection schedule is enabled */
    public boolean isEnabled() { return enabled; }
    /** @param enabled whether the garbage-collection schedule is enabled */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** @return the cron expression defining the GC job cadence */
    public String getCron() { return cron; }
    /** @param cron the cron expression to set */
    public void setCron(String cron) { this.cron = cron; }
}
