package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Request payload used to create or update a drop rule in the status-monitoring app.
 *
 * <p>A drop rule defines a filter that discards incoming log entries before they are
 * persisted. Entries matching the configured {@code level}, {@code service} and/or
 * {@code messagePattern} for the given tenant are dropped, reducing noise and storage
 * of irrelevant logs. Only active rules are applied.</p>
 */
public class DropRuleRequest {

    private UUID tenantId;

    @NotBlank(message = "Name is required")
    private String name;

    private String level;

    private String service;

    private String messagePattern;

    private boolean active = true;

    /**
     * Creates an empty drop rule request for framework/deserialization use.
     */
    public DropRuleRequest() {
    }

    /** @return the tenant this drop rule belongs to */
    public UUID getTenantId() { return tenantId; }
    /** @param tenantId the owning tenant identifier to set */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    /** @return the human-readable name of the drop rule */
    public String getName() { return name; }
    /** @param name the drop rule name to set */
    public void setName(String name) { this.name = name; }

    /** @return the severity level to match, or {@code null} for all levels */
    public String getLevel() { return level; }
    /** @param level the severity level to match to set */
    public void setLevel(String level) { this.level = level; }

    /** @return the service to match, or {@code null} for all services */
    public String getService() { return service; }
    /** @param service the service to match to set */
    public void setService(String service) { this.service = service; }

    /** @return the message pattern to match against log messages */
    public String getMessagePattern() { return messagePattern; }
    /** @param messagePattern the message pattern to match to set */
    public void setMessagePattern(String messagePattern) { this.messagePattern = messagePattern; }

    /** @return {@code true} if the rule is active and should be applied */
    public boolean isActive() { return active; }
    /** @param active whether the rule is active */
    public void setActive(boolean active) { this.active = active; }
}
