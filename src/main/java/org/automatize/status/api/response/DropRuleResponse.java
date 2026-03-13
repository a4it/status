package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

public class DropRuleResponse {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String level;
    private String service;
    private String messagePattern;
    private Boolean isActive;
    private ZonedDateTime createdDate;

    public DropRuleResponse() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public ZonedDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }
}
