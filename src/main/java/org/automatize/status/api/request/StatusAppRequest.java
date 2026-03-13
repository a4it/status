package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * <p>
 * Request object for creating or updating a status application.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate status application data for create and update operations</li>
 *   <li>Validate required fields such as name and URL slug</li>
 *   <li>Configure health check settings including URL, interval, and thresholds</li>
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
public class StatusAppRequest {

    /** The name of the status application. */
    @NotBlank(message = "Name is required")
    private String name;

    /** A description of the status application. */
    private String description;

    /** The URL-friendly slug identifier (lowercase letters, numbers, and hyphens only). */
    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    private String slug;

    /** Whether the status page is publicly accessible. */
    private Boolean isPublic = true;

    /** The current operational status of the application. */
    private String status = "OPERATIONAL";

    /** The tenant this application belongs to. */
    private UUID tenantId;

    /** The organization this application belongs to. */
    private UUID organizationId;

    /** The platform this application belongs to. */
    private UUID platformId;

    /** Whether health checking is enabled for this application. */
    private Boolean checkEnabled = false;

    /** The type of health check to perform (NONE, HTTP, TCP, etc.). */
    private String checkType = "NONE";

    /** The URL to check for health monitoring. */
    private String checkUrl;

    /** The interval in seconds between health checks. */
    private Integer checkIntervalSeconds = 60;

    /** The timeout in seconds for health check requests. */
    private Integer checkTimeoutSeconds = 10;

    /** The expected HTTP status code for successful health checks. */
    private Integer checkExpectedStatus = 200;

    /** The number of consecutive failures before marking as unhealthy. */
    private Integer checkFailureThreshold = 3;

    /**
     * Default constructor.
     */
    public StatusAppRequest() {
    }

    /**
     * Gets the application name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the application name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the URL slug.
     *
     * @return the slug
     */
    public String getSlug() {
        return slug;
    }

    /**
     * Sets the URL slug.
     *
     * @param slug the slug to set
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * Gets the public visibility flag.
     *
     * @return true if public, false otherwise
     */
    public Boolean getIsPublic() {
        return isPublic;
    }

    /**
     * Sets the public visibility flag.
     *
     * @param isPublic the public flag to set
     */
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * Gets the operational status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the operational status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the tenant ID.
     *
     * @return the tenant ID
     */
    public UUID getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant ID.
     *
     * @param tenantId the tenant ID to set
     */
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Gets the organization ID.
     *
     * @return the organization ID
     */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization ID.
     *
     * @param organizationId the organization ID to set
     */
    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Gets the platform ID.
     *
     * @return the platform ID
     */
    public UUID getPlatformId() {
        return platformId;
    }

    /**
     * Sets the platform ID.
     *
     * @param platformId the platform ID to set
     */
    public void setPlatformId(UUID platformId) {
        this.platformId = platformId;
    }

    /**
     * Gets the health check enabled flag.
     *
     * @return true if health checking is enabled, false otherwise
     */
    public Boolean getCheckEnabled() {
        return checkEnabled;
    }

    /**
     * Sets the health check enabled flag.
     *
     * @param checkEnabled the flag to set
     */
    public void setCheckEnabled(Boolean checkEnabled) {
        this.checkEnabled = checkEnabled;
    }

    /**
     * Gets the health check type.
     *
     * @return the check type
     */
    public String getCheckType() {
        return checkType;
    }

    /**
     * Sets the health check type.
     *
     * @param checkType the check type to set
     */
    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    /**
     * Gets the health check URL.
     *
     * @return the check URL
     */
    public String getCheckUrl() {
        return checkUrl;
    }

    /**
     * Sets the health check URL.
     *
     * @param checkUrl the check URL to set
     */
    public void setCheckUrl(String checkUrl) {
        this.checkUrl = checkUrl;
    }

    /**
     * Gets the health check interval in seconds.
     *
     * @return the interval in seconds
     */
    public Integer getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    /**
     * Sets the health check interval in seconds.
     *
     * @param checkIntervalSeconds the interval to set
     */
    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    /**
     * Gets the health check timeout in seconds.
     *
     * @return the timeout in seconds
     */
    public Integer getCheckTimeoutSeconds() {
        return checkTimeoutSeconds;
    }

    /**
     * Sets the health check timeout in seconds.
     *
     * @param checkTimeoutSeconds the timeout to set
     */
    public void setCheckTimeoutSeconds(Integer checkTimeoutSeconds) {
        this.checkTimeoutSeconds = checkTimeoutSeconds;
    }

    /**
     * Gets the expected HTTP status code.
     *
     * @return the expected status code
     */
    public Integer getCheckExpectedStatus() {
        return checkExpectedStatus;
    }

    /**
     * Sets the expected HTTP status code.
     *
     * @param checkExpectedStatus the expected status to set
     */
    public void setCheckExpectedStatus(Integer checkExpectedStatus) {
        this.checkExpectedStatus = checkExpectedStatus;
    }

    /**
     * Gets the failure threshold.
     *
     * @return the failure threshold
     */
    public Integer getCheckFailureThreshold() {
        return checkFailureThreshold;
    }

    /**
     * Sets the failure threshold.
     *
     * @param checkFailureThreshold the threshold to set
     */
    public void setCheckFailureThreshold(Integer checkFailureThreshold) {
        this.checkFailureThreshold = checkFailureThreshold;
    }
}