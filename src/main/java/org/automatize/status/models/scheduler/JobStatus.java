package org.automatize.status.models.scheduler;

/**
 * Lifecycle status of a scheduler job definition.
 */
public enum JobStatus {
    /** Job is active and will be evaluated for scheduling. */
    ACTIVE,
    /** Job is temporarily paused; it will not be scheduled until resumed. */
    PAUSED,
    /** Job is permanently disabled and will never be scheduled. */
    DISABLED
}
