package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

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
    @Getter
    @Setter
    private String name;

    /** A description of the status application. */
    @Getter
    @Setter
    private String description;

    /** The URL-friendly slug identifier (lowercase letters, numbers, and hyphens only). */
    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Getter
    @Setter
    private String slug;

    /** Whether the status page is publicly accessible. */
    @Getter
    @Setter
    private Boolean isPublic = true;

    /** The current operational status of the application. */
    @Getter
    @Setter
    private String status = "OPERATIONAL";

    /** The tenant this application belongs to. */
    @Getter
    @Setter
    private UUID tenantId;

    /** The organization this application belongs to. */
    @Getter
    @Setter
    private UUID organizationId;

    /** The platform this application belongs to. */
    @Getter
    @Setter
    private UUID platformId;

    /** Whether health checking is enabled for this application. */
    @Getter
    @Setter
    private Boolean checkEnabled = false;

    /** The type of health check to perform (NONE, HTTP, TCP, etc.). */
    @Getter
    @Setter
    private String checkType = "NONE";

    /** The URL to check for health monitoring. */
    @Getter
    @Setter
    private String checkUrl;

    /** The interval in seconds between health checks. */
    @Getter
    @Setter
    private Integer checkIntervalSeconds = 60;

    /** The timeout in seconds for health check requests. */
    @Getter
    @Setter
    private Integer checkTimeoutSeconds = 10;

    /** The expected HTTP status code for successful health checks. */
    @Getter
    @Setter
    private Integer checkExpectedStatus = 200;

    /** The number of consecutive failures before marking as unhealthy. */
    @Getter
    @Setter
    private Integer checkFailureThreshold = 3;

    /**
     * Default constructor.
     */
    public StatusAppRequest() {
    }
}
