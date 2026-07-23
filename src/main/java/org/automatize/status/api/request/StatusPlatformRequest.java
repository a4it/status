package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

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
    @Getter
    @Setter
    private String name;

    /** A description of the platform. */
    @Getter
    @Setter
    private String description;

    /** The URL-friendly slug identifier (lowercase letters, numbers, and hyphens only). */
    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Getter
    @Setter
    private String slug;

    /** URL to the platform logo image. */
    @Getter
    @Setter
    private String logoUrl;

    /** External website URL for the platform. */
    @Getter
    @Setter
    private String websiteUrl;

    /** The current operational status of the platform. */
    @Getter
    @Setter
    private String status = "OPERATIONAL";

    /** Whether the platform is publicly visible. */
    @Getter
    @Setter
    private Boolean isPublic = true;

    /** Display order position for sorting platforms. */
    @Getter
    @Setter
    private Integer position = 0;

    /** The tenant this platform belongs to. */
    @Getter
    @Setter
    private UUID tenantId;

    /** The organization this platform belongs to. */
    @Getter
    @Setter
    private UUID organizationId;

    /**
     * Default constructor.
     */
    public StatusPlatformRequest() {
    }
}
