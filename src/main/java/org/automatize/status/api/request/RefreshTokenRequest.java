package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * Request object for refreshing an authentication token.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate the refresh token for token renewal requests</li>
 *   <li>Validate that the refresh token is provided</li>
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
public class RefreshTokenRequest {

    /** The refresh token to use for obtaining a new access token. */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    /**
     * Default constructor.
     */
    public RefreshTokenRequest() {
    }

    /**
     * Gets the refresh token.
     *
     * @return the refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Sets the refresh token.
     *
     * @param refreshToken the refresh token to set
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}