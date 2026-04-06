package org.automatize.status.models.scheduler;

/**
 * Specifies where the API key is sent in an HTTP request.
 */
public enum ApiKeyLocation {
    /** API key is sent as an HTTP request header. */
    HEADER,
    /** API key is appended as a URL query parameter. */
    QUERY_PARAM
}
