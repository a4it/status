package org.automatize.status.api.request;

import java.util.UUID;

/**
 * <p>
 * Request object for switching the active tenant/organization context of the current session.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate the target tenant and organization for a context switch</li>
 *   <li>Support the multi-tenant Tenant &rarr; Organization &rarr; User hierarchy by
 *       letting a user change which tenant/organization scope they operate under</li>
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
public class SwitchContextRequest {

    /** The tenant to switch the active context to. */
    private UUID tenantId;

    /** The organization to switch the active context to. */
    private UUID organizationId;

    /**
     * Default constructor.
     */
    public SwitchContextRequest() {
    }

    /**
     * Gets the target tenant ID.
     *
     * @return the tenant ID
     */
    public UUID getTenantId() {
        return tenantId;
    }

    /**
     * Sets the target tenant ID.
     *
     * @param tenantId the tenant ID to set
     */
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Gets the target organization ID.
     *
     * @return the organization ID
     */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the target organization ID.
     *
     * @param organizationId the organization ID to set
     */
    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}
