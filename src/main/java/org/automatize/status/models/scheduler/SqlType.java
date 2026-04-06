package org.automatize.status.models.scheduler;

/**
 * Classifies the SQL statement executed by a scheduler SQL job.
 */
public enum SqlType {
    /** Data Manipulation Language (INSERT, UPDATE, DELETE, MERGE). */
    DML,
    /** Data Definition Language (CREATE, ALTER, DROP, TRUNCATE). */
    DDL,
    /** A SELECT query whose result set can optionally be captured. */
    QUERY
}
