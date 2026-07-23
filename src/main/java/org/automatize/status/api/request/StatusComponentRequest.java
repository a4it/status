package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Request object for creating or updating a status component.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate component data for create and update operations</li>
 *   <li>Validate required fields such as app ID and component name</li>
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
public class StatusComponentRequest {

    /** The ID of the parent status application. */
    @NotNull(message = "App ID is required")
    @Getter
    @Setter
    private UUID appId;

    /** The name of the component. */
    @NotBlank(message = "Name is required")
    @Getter
    @Setter
    private String name;

    /** A description of the component. */
    @Getter
    @Setter
    private String description;

    /** The current operational status of the component. */
    @Getter
    @Setter
    private String status = "OPERATIONAL";

    /** The display position/order of the component. */
    @Getter
    @Setter
    private Integer position = 0;

    /** The group name for organizing related components. */
    @Getter
    @Setter
    private String groupName;

    /** Whether to inherit health check settings from the parent application. */
    @Getter
    @Setter
    private Boolean checkInheritFromApp = true;

    /** Whether health checking is enabled for this component. */
    @Getter
    @Setter
    private Boolean checkEnabled = false;

    /** The type of health check to perform. */
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
    public StatusComponentRequest() {
    }
}
