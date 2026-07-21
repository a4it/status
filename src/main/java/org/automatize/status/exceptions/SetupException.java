package org.automatize.status.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>
 * Thrown when a setup-wizard operation cannot be completed, such as failing to
 * persist configuration to the application properties file on disk.
 * </p>
 *
 * <p>
 * Maps to HTTP 500 (Internal Server Error) when propagated out of a REST controller.
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SetupException extends RuntimeException {

    /**
     * Creates a new exception with the given message and underlying cause.
     *
     * @param message the detail message describing the failed setup operation
     * @param cause   the underlying cause of the failure
     */
    public SetupException(String message, Throwable cause) {
        super(message, cause);
    }
}
