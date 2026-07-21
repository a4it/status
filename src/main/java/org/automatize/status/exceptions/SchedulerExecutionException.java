package org.automatize.status.exceptions;

/**
 * <p>
 * Thrown when a scheduler job cannot be executed due to a technical failure
 * (for example an SSL context that cannot be initialised), as opposed to a
 * business or validation error.
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
public class SchedulerExecutionException extends RuntimeException {

    /**
     * Creates a new exception with the given message and underlying cause.
     *
     * @param message the detail message describing the failure
     * @param cause   the underlying cause
     */
    public SchedulerExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
