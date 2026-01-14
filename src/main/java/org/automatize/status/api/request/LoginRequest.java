package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request object for user authentication.
 * <p>
 * Contains the credentials required for user login to the status monitoring system.
 * </p>
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