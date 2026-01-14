package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request object for creating or updating a tenant.
 * <p>
 * Tenants represent the top-level organizational unit in the multi-tenant hierarchy.
 * Each tenant can have multiple organizations.
 * </p>
 */
public class TenantRequest {

    /** The name of the tenant. */
    @NotBlank(message = "Name is required")
    private String name;

    /** Whether the tenant is active. */
    private Boolean isActive = true;

    /**
     * Default constructor.
     */
    public TenantRequest() {
    }

    /**
     * Gets the tenant name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the tenant name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the active status.
     *
     * @return true if active, false otherwise
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Sets the active status.
     *
     * @param isActive the active status to set
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}