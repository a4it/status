package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request object for refreshing an authentication token.
 * <p>
 * Used to obtain a new access token using a valid refresh token.
 * </p>
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