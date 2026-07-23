package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Response object containing status component details.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide component information including status and grouping</li>
 *   <li>Include health check configuration and results</li>
 *   <li>Track consecutive failures and check history</li>
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
public class StatusComponentResponse {

    /** The unique identifier of the component. */
    @Getter
    @Setter
    private UUID id;

    /** The ID of the parent application. */
    @Getter
    @Setter
    private UUID appId;

    /** The name of the component. */
    @Getter
    @Setter
    private String name;

    /** The description of the component. */
    @Getter
    @Setter
    private String description;

    /** The current operational status. */
    @Getter
    @Setter
    private String status;

    /** The group name for organizing components. */
    @Getter
    @Setter
    private String groupName;

    /** The display position/order. */
    @Getter
    @Setter
    private Integer position;

    /** Whether to inherit health check settings from parent app. */
    @Getter
    @Setter
    private Boolean checkInheritFromApp;

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
    public StatusComponentResponse() {
    }
}
