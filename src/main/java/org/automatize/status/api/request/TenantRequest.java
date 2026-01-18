package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * Request object for creating or updating a tenant entity.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate tenant data for create and update operations</li>
 *   <li>Validate required tenant fields such as name</li>
 *   <li>Provide tenant activation status management</li>
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