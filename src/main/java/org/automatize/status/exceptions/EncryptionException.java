package org.automatize.status.exceptions;

/**
 * <p>
 * Thrown when a symmetric encryption or decryption operation cannot be
 * completed, for example when the cipher cannot be initialised or the input
 * is not valid ciphertext.
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
public class EncryptionException extends RuntimeException {

    /**
     * Creates a new exception with the given message and underlying cause.
     *
     * @param message the detail message describing the failed operation
     * @param cause   the underlying cryptographic failure
     */
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
