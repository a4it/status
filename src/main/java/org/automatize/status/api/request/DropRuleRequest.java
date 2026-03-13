package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class DropRuleRequest {

    private UUID tenantId;

    @NotBlank(message = "Name is required")
    private String name;

    private String level;

    private String service;

    private String messagePattern;

    private boolean active = true;

    public DropRuleRequest() {
    }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getMessagePattern() { return messagePattern; }
    public void setMessagePattern(String messagePattern) { this.messagePattern = messagePattern; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
