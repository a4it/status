package org.automatize.status.exceptions;

/**
 * <p>
 * Thrown when a scheduler job cannot be registered with the task scheduler,
 * for example when its cron expression is invalid or the scheduling backend
 * rejects the trigger.
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
public class JobSchedulingException extends RuntimeException {

    /**
     * Creates a new exception with the given message and underlying cause.
     *
     * @param message the detail message describing the scheduling failure
     * @param cause   the underlying failure
     */
    public JobSchedulingException(String message, Throwable cause) {
        super(message, cause);
    }
}
