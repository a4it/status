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
    @Getter
    @Setter
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

    /** The current operational status. */
    @Getter
    @Setter
    private String status;

    /** Whether the platform is publicly visible. */
    @Getter
    @Setter
    private Boolean isPublic;

    /** Display order position. */
    @Getter
    @Setter
    private Integer position;

    /** List of applications belonging to this platform. */
    @Getter
    @Setter
    private List<StatusAppResponse> apps;

    /** When the platform was last updated. */
    @Getter
    @Setter
    private ZonedDateTime lastUpdated;

    /** The ID of the tenant this platform belongs to. */
    @Getter
    @Setter
    private UUID tenantId;

    /** The ID of the organization this platform belongs to. */
    @Getter
    @Setter
    private UUID organizationId;

    /**
     * Default constructor.
     */
    public StatusPlatformResponse() {
    }
}
