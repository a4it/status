package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * Request object for user authentication containing login credentials.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate username and password for authentication requests</li>
 *   <li>Validate that required credentials are provided</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
public class LoginRequest {

    /** The username for authentication. */
    @NotBlank(message = "Username is required")
    private String username;

    /** The password for authentication. */
    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Default constructor.
     */
    public LoginRequest() {
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
}