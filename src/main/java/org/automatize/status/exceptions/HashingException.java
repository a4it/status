package org.automatize.status.exceptions;

/**
 * Thrown when a cryptographic hashing operation cannot be performed,
 * for example when the required message digest algorithm is unavailable.
 */
public class HashingException extends RuntimeException {

    /**
     * Creates a new exception with the given message and underlying cause.
     *
     * @param message the detail message describing the hashing failure
     * @param cause   the underlying cryptographic failure
     */
    public HashingException(String message, Throwable cause) {
        super(message, cause);
    }
}
