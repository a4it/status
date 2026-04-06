package org.automatize.status.models.scheduler;

/**
 * Outcome status of a single job execution run.
 */
public enum JobRunStatus {
    /** The job is currently executing. */
    RUNNING,
    /** The job completed without errors. */
    SUCCESS,
    /** The job terminated with an error. */
    FAILURE,
    /** The job exceeded its configured timeout. */
    TIMEOUT,
    /** The job was cancelled before it could complete. */
    CANCELLED,
    /** Execution was skipped (e.g. concurrent run prevention). */
    SKIPPED
}
