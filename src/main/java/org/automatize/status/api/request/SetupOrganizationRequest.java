package org.automatize.status.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * <p>
 * Request object for creating an organization during the first-run setup wizard
 * of the status monitoring system.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Capture the name, contact email and type of the organization being created</li>
 *   <li>Validate that the required fields are present and the email is well-formed</li>
 *   <li>Bind the new organization to its parent tenant in the multi-tenant hierarchy</li>
 * </ul>
 * </p>
 *
 * @author Tim De Smedt
 */
public class SetupOrganizationRequest {

    /** The organization name. */
    @NotBlank(message = "Organization name is required")
    private String name;

    /** The organization contact email address. */
    @Email(message = "Email must be valid")
    private String email;

    /** The organization type classifier. */
    @NotBlank(message = "Organization type is required")
    private String organizationType;

    /** The parent tenant the organization belongs to. */
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    /** @return the organization name */
    public String getName() { return name; }
    /** @param name the organization name to set */
    public void setName(String name) { this.name = name; }

    /** @return the organization contact email address */
    public String getEmail() { return email; }
    /** @param email the organization contact email address to set */
    public void setEmail(String email) { this.email = email; }

    /** @return the organization type classifier */
    public String getOrganizationType() { return organizationType; }
    /** @param organizationType the organization type classifier to set */
    public void setOrganizationType(String organizationType) { this.organizationType = organizationType; }

    /** @return the parent tenant identifier */
    public UUID getTenantId() { return tenantId; }
    /** @param tenantId the parent tenant identifier to set */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
}
