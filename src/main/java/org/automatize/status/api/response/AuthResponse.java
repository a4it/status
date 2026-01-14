package org.automatize.status.api.response;

import java.util.UUID;

/**
 * Response object for authentication operations.
 * <p>
 * Contains JWT tokens and user information returned after successful
 * authentication or token refresh operations.
 * </p>
 */
public class AuthResponse {

    /** The JWT access token for API authentication. */
    private String accessToken;

    /** The refresh token for obtaining new access tokens. */
    private String refreshToken;

    /** The type of token (always "Bearer"). */
    private String tokenType = "Bearer";

    /** Token expiration time in seconds. */
    private Long expiresIn;

    /** The unique identifier of the authenticated user. */
    private UUID userId;

    /** The username of the authenticated user. */
    private String username;

    /** The email address of the authenticated user. */
    private String email;

    /** The role assigned to the authenticated user. */
    private String role;

    /** The organization ID the user belongs to. */
    private UUID organizationId;

    /**
     * Default constructor.
     */
    public AuthResponse() {
    }

    /**
     * Gets the access token.
     *
     * @return the access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token.
     *
     * @param accessToken the access token to set
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
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

    /**
     * Gets the token type.
     *
     * @return the token type
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Sets the token type.
     *
     * @param tokenType the token type to set
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Gets the expiration time in seconds.
     *
     * @return the expiration time
     */
    public Long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Sets the expiration time in seconds.
     *
     * @param expiresIn the expiration time to set
     */
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Gets the user ID.
     *
     * @return the user ID
     */
    public UUID getUserId() {
        return userId;
    }

    /**
     * Sets the user ID.
     *
     * @param userId the user ID to set
     */
    public void setUserId(UUID userId) {
        this.userId = userId;
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
     * Gets the email.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email.
     *
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the role.
     *
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the role.
     *
     * @param role the role to set
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Gets the organization ID.
     *
     * @return the organization ID
     */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization ID.
     *
     * @param organizationId the organization ID to set
     */
    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}