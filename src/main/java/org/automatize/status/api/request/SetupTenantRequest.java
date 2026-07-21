package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * Request object for creating the root tenant during the first-run setup wizard
 * of the status monitoring system.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Capture the name of the top-level tenant in the multi-tenant hierarchy</li>
 *   <li>Validate that a tenant name is provided</li>
 * </ul>
 * </p>
 *
 * @author Tim De Smedt
 */
public class SetupTenantRequest {

    /** The tenant name. */
    @NotBlank(message = "Tenant name is required")
    private String name;

    /** @return the tenant name */
    public String getName() { return name; }
    /** @param name the tenant name to set */
    public void setName(String name) { this.name = name; }
}
