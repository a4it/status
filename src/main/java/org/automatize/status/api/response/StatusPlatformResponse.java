package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response object containing status platform details.
 * <p>
 * Provides comprehensive information about a status platform including
 * its associated applications and overall status.
 * </p>
 */
public class StatusPlatformResponse {

    /** The unique identifier of the platform. */
    private UUID id;

    /** The name of the platform. */
    private String name;

    /** The description of the platform. */
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public List<StatusAppResponse> getApps() {
        return apps;
    }

    public void setApps(List<StatusAppResponse> apps) {
        this.apps = apps;
    }

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}
