package org.automatize.status.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>
 * Thrown when creating or updating an entity would violate a uniqueness
 * constraint (e.g. a duplicate name or email).
 * </p>
 *
 * <p>
 * Maps to HTTP 409 (Conflict) when propagated out of a REST controller.
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    /**
     * Creates a new exception with the given message.
     *
     * @param message the detail message describing the conflicting resource
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}
