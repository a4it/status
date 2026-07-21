package org.automatize.status.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>
 * Thrown when an operation cannot be completed because it would violate a
 * business rule or the current state of the domain (e.g. deleting an
 * organization that still has active users).
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
public class BusinessRuleException extends RuntimeException {

    /**
     * Creates a new exception with the given message.
     *
     * @param message the detail message describing the violated rule
     */
    public BusinessRuleException(String message) {
        super(message);
    }
}
