package org.automatize.status.models.scheduler;

/**
 * HTTP methods supported by the scheduler REST job type.
 */
public enum HttpMethod {
    /** HTTP GET: retrieve a resource. */
    GET,
    /** HTTP POST: submit data to create or process a resource. */
    POST,
    /** HTTP PUT: replace a resource with the request payload. */
    PUT,
    /** HTTP PATCH: apply a partial update to a resource. */
    PATCH,
    /** HTTP DELETE: remove a resource. */
    DELETE,
    /** HTTP HEAD: retrieve headers only, without a response body. */
    HEAD,
    /** HTTP OPTIONS: query the communication options for a resource. */
    OPTIONS
}
