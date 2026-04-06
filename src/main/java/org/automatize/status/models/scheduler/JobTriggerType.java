package org.automatize.status.models.scheduler;

/**
 * Describes what initiated a particular job run.
 */
public enum JobTriggerType {
    /** Run was triggered automatically by the cron schedule. */
    SCHEDULED,
    /** Run was triggered manually by a user or API call. */
    MANUAL,
    /** Run is a retry attempt following a previous failure. */
    RETRY
}
