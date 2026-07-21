package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * <p>
 * Request object for creating or updating a status platform.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate platform data for create and update operations</li>
 *   <li>Validate required fields such as name and URL slug</li>
 *   <li>Provide tenant and organization association configuration</li>
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
public class StatusPlatformRequest {

    /** The name of the platform. */
    @NotBlank(message = "Name is required")
    private String name;

    /** A description of the platform. */
    private String description;

    /** The URL-friendly slug identifier (lowercase letters, numbers, and hyphens only). */
    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    private String slug;

    /** URL to the platform logo image. */
    private String logoUrl;

    /** External website URL for the platform. */
    private String websiteUrl;

    /** The current operational status of the platform. */
    private String status = "OPERATIONAL";

    /** Whether the platform is publicly visible. */
    private Boolean isPublic = true;

    /** Display order position for sorting platforms. */
    private Integer position = 0;

    /** The tenant this platform belongs to. */
    private UUID tenantId;

    /** The organization this platform belongs to. */
    private UUID organizationId;

    /**
     * Default constructor.
     */
    public StatusPlatformRequest() {
    }

    /**
     * Gets the platform name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the platform name.
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
     * Gets the URL-friendly slug identifier.
     *
     * @return the slug
     */
    public String getSlug() {
        return slug;
    }

    /**
     * Sets the URL-friendly slug identifier.
     *
     * @param slug the slug to set
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * Gets the platform logo image URL.
     *
     * @return the logo URL
     */
    public String getLogoUrl() {
        return logoUrl;
    }

    /**
     * Sets the platform logo image URL.
     *
     * @param logoUrl the logo URL to set
     */
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    /**
     * Gets the external website URL.
     *
     * @return the website URL
     */
    public String getWebsiteUrl() {
        return websiteUrl;
    }

    /**
     * Sets the external website URL.
     *
     * @param websiteUrl the website URL to set
     */
    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
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
     * Gets the display order position.
     *
     * @return the position
     */
    public Integer getPosition() {
        return position;
    }

    /**
     * Sets the display order position.
     *
     * @param position the position to set
     */
    public void setPosition(Integer position) {
        this.position = position;
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
}
