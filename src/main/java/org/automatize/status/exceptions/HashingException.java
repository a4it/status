package org.automatize.status.exceptions;

/**
 * Thrown when a cryptographic hashing operation cannot be performed,
 * for example when the required message digest algorithm is unavailable.
 */
public class HashingException extends RuntimeException {

    public HashingException(String message, Throwable cause) {
        super(message, cause);
    }
}
