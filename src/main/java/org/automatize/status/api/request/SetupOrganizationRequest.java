package org.automatize.status.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class SetupOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    private String name;

    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Organization type is required")
    private String organizationType;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOrganizationType() { return organizationType; }
    public void setOrganizationType(String organizationType) { this.organizationType = organizationType; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
}
