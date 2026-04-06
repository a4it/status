package org.automatize.status.models.scheduler;

/**
 * Defines the type of work a scheduler job performs.
 */
public enum JobType {
    /** Executes an operating-system program or script. */
    PROGRAM,
    /** Executes a SQL statement against a JDBC datasource. */
    SQL,
    /** Invokes an HTTP/REST endpoint. */
    REST,
    /** Invokes a SOAP web-service operation. */
    SOAP
}
