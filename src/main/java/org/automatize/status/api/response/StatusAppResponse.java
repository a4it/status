package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Response object containing status application details.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide comprehensive application information including components</li>
 *   <li>Track current incidents and upcoming maintenance windows</li>
 *   <li>Include health check configuration and results</li>
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
public class StatusAppResponse {

    /** The unique identifier of the application. */
    @Getter
    @Setter
    private UUID id;

    /** The name of the application. */
    @Getter
    @Setter
    private String name;

    /** The description of the application. */
    @Getter
    @Setter
    private String description;

    /** The URL-friendly slug identifier. */
    @Getter
    @Setter
    private String slug;

    /** The current operational status. */
    @Getter
    @Setter
    private String status;

    /** Whether the status page is publicly accessible. */
    @Getter
    @Setter
    private Boolean isPublic;

    /** List of components belonging to this application. */
    @Getter
    @Setter
    private List<StatusComponentResponse> components;

    /** The current active incident, if any. */
    @Getter
    @Setter
    private StatusIncidentResponse currentIncident;

    /** List of upcoming scheduled maintenances. */
    @Getter
    @Setter
    private List<StatusMaintenanceResponse> upcomingMaintenances;

    /** When the status was last updated. */
    @Getter
    @Setter
    private ZonedDateTime lastUpdated;

    /** The ID of the platform this application belongs to. */
    @Getter
    @Setter
    private UUID platformId;

    /** The name of the platform this application belongs to. */
    @Getter
    @Setter
    private String platformName;

    /** Whether health checking is enabled. */
    @Getter
    @Setter
    private Boolean checkEnabled;

    /** The type of health check performed. */
    @Getter
    @Setter
    private String checkType;

    /** The URL being checked for health monitoring. */
    @Getter
    @Setter
    private String checkUrl;

    /** The interval between health checks in seconds. */
    @Getter
    @Setter
    private Integer checkIntervalSeconds;

    /** The timeout for health check requests in seconds. */
    @Getter
    @Setter
    private Integer checkTimeoutSeconds;

    /** The expected HTTP status code for successful checks. */
    @Getter
    @Setter
    private Integer checkExpectedStatus;

    /** Number of failures before marking unhealthy. */
    @Getter
    @Setter
    private Integer checkFailureThreshold;

    /** When the last health check was performed. */
    @Getter
    @Setter
    private ZonedDateTime lastCheckAt;

    /** Whether the last health check was successful. */
    @Getter
    @Setter
    private Boolean lastCheckSuccess;

    /** The message from the last health check. */
    @Getter
    @Setter
    private String lastCheckMessage;

    /** The current count of consecutive failures. */
    @Getter
    @Setter
    private Integer consecutiveFailures;

    /** API key for event logging authentication. */
    @Getter
    @Setter
    private String apiKey;

    /**
     * Default constructor.
     */
    public StatusAppResponse() {
    }
}
