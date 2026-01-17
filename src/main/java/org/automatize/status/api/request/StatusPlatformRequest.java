package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Request object for creating or updating a status platform.
 * <p>
 * A status platform represents a higher-level grouping that can contain
 * multiple status applications.
 * </p>
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
