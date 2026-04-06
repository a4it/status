package org.automatize.status.models.scheduler;

/**
 * SOAP protocol version used by the scheduler SOAP job type.
 */
public enum SoapVersion {
    /** SOAP 1.1 (uses text/xml content type). */
    V1_1,
    /** SOAP 1.2 (uses application/soap+xml content type). */
    V1_2
}
