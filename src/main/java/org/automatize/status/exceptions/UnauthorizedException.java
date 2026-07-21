package org.automatize.status.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>
 * Thrown when a request cannot be authenticated, for example when an API key,
 * refresh token or authorization header is missing or invalid.
 * </p>
 *
 * <p>
 * Maps to HTTP 401 (Unauthorized) when propagated out of a REST controller.
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    /**
     * Creates a new exception with the given message.
     *
     * @param message the detail message describing the authentication failure
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
