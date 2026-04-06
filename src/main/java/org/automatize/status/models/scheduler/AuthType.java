package org.automatize.status.models.scheduler;

/**
 * Authentication schemes supported by REST and SOAP scheduler job types.
 */
public enum AuthType {
    /** No authentication. */
    NONE,
    /** HTTP Basic authentication (username + password). */
    BASIC,
    /** HTTP Bearer token authentication. */
    BEARER,
    /** API key authentication (header or query parameter). */
    API_KEY,
    /** OAuth 2.0 Client Credentials flow. */
    OAUTH2_CLIENT
}
