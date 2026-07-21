package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Response object describing a log drop rule used to filter out unwanted log entries.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Identify the tenant-scoped drop rule and its human-readable name</li>
 *   <li>Describe the matching criteria (level, service, message pattern)</li>
 *   <li>Expose whether the rule is active and when it was created</li>
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
public class DropRuleResponse {

    /** The unique identifier of the drop rule. */
    private UUID id;

    /** The ID of the tenant this rule belongs to. */
    private UUID tenantId;

    /** The human-readable name of the rule. */
    private String name;

    /** The log level this rule matches. */
    private String level;

    /** The service name this rule matches. */
    private String service;

    /** The message pattern this rule matches. */
    private String messagePattern;

    /** Whether the rule is currently active. */
    private Boolean isActive;

    /** When the rule was created. */
    private ZonedDateTime createdDate;

    /**
     * Default constructor.
     */
    public DropRuleResponse() {
    }

    /** Gets the ID. @return the ID */
    public UUID getId() { return id; }
    /** Sets the ID. @param id the ID to set */
    public void setId(UUID id) { this.id = id; }

    /** Gets the tenant ID. @return the tenant ID */
    public UUID getTenantId() { return tenantId; }
    /** Sets the tenant ID. @param tenantId the tenant ID to set */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    /** Gets the rule name. @return the rule name */
    public String getName() { return name; }
    /** Sets the rule name. @param name the rule name to set */
    public void setName(String name) { this.name = name; }

    /** Gets the log level. @return the log level */
    public String getLevel() { return level; }
    /** Sets the log level. @param level the log level to set */
    public void setLevel(String level) { this.level = level; }

    /** Gets the service name. @return the service name */
    public String getService() { return service; }
    /** Sets the service name. @param service the service name to set */
    public void setService(String service) { this.service = service; }

    /** Gets the message pattern. @return the message pattern */
    public String getMessagePattern() { return messagePattern; }
    /** Sets the message pattern. @param messagePattern the message pattern to set */
    public void setMessagePattern(String messagePattern) { this.messagePattern = messagePattern; }

    /** Gets the active flag. @return true if the rule is active, false otherwise */
    public Boolean getIsActive() { return isActive; }
    /** Sets the active flag. @param isActive the active flag to set */
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    /** Gets the created date. @return the created date */
    public ZonedDateTime getCreatedDate() { return createdDate; }
    /** Sets the created date. @param createdDate the created date to set */
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }
}
