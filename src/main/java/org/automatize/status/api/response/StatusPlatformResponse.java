package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Response object containing status platform details.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide comprehensive platform information</li>
 *   <li>Include associated applications and their status</li>
 *   <li>Track platform visibility and organization association</li>
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
public class StatusPlatformResponse {

    /** The unique identifier of the platform. */
    private UUID id;

    /** The name of the platform. */
    @Getter
    @Setter
    private String name;

    /** The description of the platform. */
    @Getter
    @Setter
    private String description;

    /** The URL-friendly slug identifier. */
    private String slug;

    /** URL to the platform logo image. */
    private String logoUrl;

    /** External website URL for the platform. */
    private String websiteUrl;

    /** The current operational status. */
    private String status;

    /** Whether the platform is publicly visible. */
    private Boolean isPublic;

    /** Display order position. */
    private Integer position;

    /** List of applications belonging to this platform. */
    private List<StatusAppResponse> apps;

    /** When the platform was last updated. */
    private ZonedDateTime lastUpdated;

    /** The ID of the tenant this platform belongs to. */
    private UUID tenantId;

    /** The ID of the organization this platform belongs to. */
    private UUID organizationId;

    /**
     * Default constructor.
     */
    public StatusPlatformResponse() {
    }

    /**
     * Gets the platform ID.
     *
     * @return the platform ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the platform ID.
     *
     * @param id the platform ID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the URL-friendly slug.
     *
     * @return the slug
     */
    public String getSlug() {
        return slug;
    }

    /**
     * Sets the URL-friendly slug.
     *
     * @param slug the slug to set
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * Gets the logo URL.
     *
     * @return the logo URL
     */
    public String getLogoUrl() {
        return logoUrl;
    }

    /**
     * Sets the logo URL.
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
     * Gets the current operational status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current operational status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the public visibility flag.
     *
     * @return true if the platform is publicly visible, false otherwise
     */
    public Boolean getIsPublic() {
        return isPublic;
    }

    /**
     * Sets the public visibility flag.
     *
     * @param isPublic the visibility flag to set
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
     * Gets the list of applications belonging to this platform.
     *
     * @return the applications
     */
    public List<StatusAppResponse> getApps() {
        return apps;
    }

    /**
     * Sets the list of applications belonging to this platform.
     *
     * @param apps the applications to set
     */
    public void setApps(List<StatusAppResponse> apps) {
        this.apps = apps;
    }

    /**
     * Gets the last updated timestamp.
     *
     * @return the last updated timestamp
     */
    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Sets the last updated timestamp.
     *
     * @param lastUpdated the last updated timestamp to set
     */
    public void setLastUpdated(ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Gets the ID of the tenant this platform belongs to.
     *
     * @return the tenant ID
     */
    public UUID getTenantId() {
        return tenantId;
    }

    /**
     * Sets the ID of the tenant this platform belongs to.
     *
     * @param tenantId the tenant ID to set
     */
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Gets the ID of the organization this platform belongs to.
     *
     * @return the organization ID
     */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the ID of the organization this platform belongs to.
     *
     * @param organizationId the organization ID to set
     */
    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}
