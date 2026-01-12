package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

public class TenantRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private Boolean isActive = true;

    public TenantRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}