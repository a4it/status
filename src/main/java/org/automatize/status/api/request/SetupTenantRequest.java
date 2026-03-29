package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

public class SetupTenantRequest {

    @NotBlank(message = "Tenant name is required")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
