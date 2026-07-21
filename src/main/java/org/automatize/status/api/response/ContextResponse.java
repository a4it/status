package org.automatize.status.api.response;

import java.util.UUID;

/**
 * <p>
 * Response object describing the authenticated user's active tenant and organization context.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Carry the access token issued for the selected context</li>
 *   <li>Identify the currently selected tenant and organization</li>
 *   <li>Expose superadmin status and whether a context has been selected</li>
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
public class ContextResponse {

    /** The JWT access token issued for the selected context. */
    private String accessToken;

    /** The ID of the currently selected tenant. */
    private UUID tenantId;

    /** The name of the currently selected tenant. */
    private String tenantName;

    /** The ID of the currently selected organization. */
    private UUID organizationId;

    /** The name of the currently selected organization. */
    private String organizationName;

    /** Whether the authenticated user is a superadmin. */
    private boolean superadmin;

    /** Whether the user has selected an active tenant/organization context. */
    private boolean hasSelectedContext;

    /**
     * Default constructor.
     */
    public ContextResponse() {
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
     * Gets the tenant ID.
     *
     * @return the tenant ID
     */
    public UUID getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant ID.
     *
     * @param tenantId the tenant ID to set
     */
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Gets the tenant name.
     *
     * @return the tenant name
     */
    public String getTenantName() {
        return tenantName;
    }

    /**
     * Sets the tenant name.
     *
     * @param tenantName the tenant name to set
     */
    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
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

    /**
     * Gets the organization name.
     *
     * @return the organization name
     */
    public String getOrganizationName() {
        return organizationName;
    }

    /**
     * Sets the organization name.
     *
     * @param organizationName the organization name to set
     */
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    /**
     * Gets the superadmin flag.
     *
     * @return true if the user is a superadmin, false otherwise
     */
    public boolean isSuperadmin() {
        return superadmin;
    }

    /**
     * Sets the superadmin flag.
     *
     * @param superadmin the superadmin flag to set
     */
    public void setSuperadmin(boolean superadmin) {
        this.superadmin = superadmin;
    }

    /**
     * Gets whether a context has been selected.
     *
     * @return true if a context has been selected, false otherwise
     */
    public boolean isHasSelectedContext() {
        return hasSelectedContext;
    }

    /**
     * Sets whether a context has been selected.
     *
     * @param hasSelectedContext the flag to set
     */
    public void setHasSelectedContext(boolean hasSelectedContext) {
        this.hasSelectedContext = hasSelectedContext;
    }
}
