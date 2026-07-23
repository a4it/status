package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Entity representing a component or sub-service of a status application.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Break down applications into smaller, independently monitored pieces</li>
 *   <li>Support custom or inherited health check configuration</li>
 *   <li>Enable logical grouping for organization on status pages</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusApp
 * @see StatusIncidentComponent
 * @see StatusMaintenanceComponent
 */
@Entity
@Table(name = "status_components")
@EntityListeners(AuditTimestampListener.class)
public class StatusComponent implements Auditable {

    /**
     * Unique identifier for the component.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    @Getter
    @Setter
    private UUID id;

    /**
     * The parent application that this component belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    @Getter
    @Setter
    private StatusApp app;

    /**
     * Display name of the component.
     */
    @Column(name = "name", nullable = false, length = 255)
    @Getter
    @Setter
    private String name;

    /**
     * Detailed description of the component and its function.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    @Getter
    @Setter
    private String description;

    /**
     * Current operational status of the component.
     * Defaults to "OPERATIONAL". Common values include OPERATIONAL, DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE.
     */
    @Column(name = "status", nullable = false, length = 50)
    @Getter
    @Setter
    private String status = "OPERATIONAL";

    /**
     * Display position for ordering components on the status page.
     * Lower values appear first. Defaults to 0.
     */
    @Column(name = "position")
    @Getter
    @Setter
    private Integer position = 0;

    /**
     * Optional group name for organizing related components together.
     */
    @Column(name = "group_name", length = 255)
    @Getter
    @Setter
    private String groupName;

    /**
     * Flag indicating whether to inherit health check configuration from the parent application.
     * Defaults to true.
     */
    @Column(name = "check_inherit_from_app")
    @Getter
    @Setter
    private Boolean checkInheritFromApp = true;

    /**
     * Flag indicating whether automated health checks are enabled for this component.
     * Only applies when checkInheritFromApp is false. Defaults to false.
     */
    @Column(name = "check_enabled")
    @Getter
    @Setter
    private Boolean checkEnabled = false;

    /**
     * Type of health check to perform for this component.
     * Defaults to "NONE". Common values include NONE, HTTP, HTTPS, TCP.
     */
    @Column(name = "check_type", length = 50)
    @Getter
    @Setter
    private String checkType = "NONE";

    /**
     * URL endpoint to check for health status of this component.
     * Used when check type is HTTP or HTTPS.
     */
    @Column(name = "check_url", length = 500)
    @Getter
    @Setter
    private String checkUrl;

    /**
     * Interval in seconds between health check executions.
     * Defaults to 60 seconds.
     */
    @Column(name = "check_interval_seconds")
    @Getter
    @Setter
    private Integer checkIntervalSeconds = 60;

    /**
     * Timeout in seconds for health check requests.
     * Defaults to 10 seconds.
     */
    @Column(name = "check_timeout_seconds")
    @Getter
    @Setter
    private Integer checkTimeoutSeconds = 10;

    /**
     * Expected HTTP status code for successful health checks.
     * Defaults to 200.
     */
    @Column(name = "check_expected_status")
    @Getter
    @Setter
    private Integer checkExpectedStatus = 200;

    /**
     * Number of consecutive failures required before marking the component as down.
     * Defaults to 3 to prevent false positives from transient issues.
     */
    @Column(name = "check_failure_threshold")
    @Getter
    @Setter
    private Integer checkFailureThreshold = 3;

    /**
     * Timestamp of the last health check execution.
     */
    @Column(name = "last_check_at")
    @Getter
    @Setter
    private ZonedDateTime lastCheckAt;

    /**
     * Flag indicating whether the last health check was successful.
     */
    @Column(name = "last_check_success")
    @Getter
    @Setter
    private Boolean lastCheckSuccess;

    /**
     * Message from the last health check providing details about the result.
     */
    @Column(name = "last_check_message", length = 1000)
    @Getter
    @Setter
    private String lastCheckMessage;

    /**
     * Count of consecutive failed health checks.
     * Resets to 0 on successful check.
     */
    @Column(name = "consecutive_failures")
    @Getter
    @Setter
    private Integer consecutiveFailures = 0;

    /**
     * API key for authenticating event logging requests.
     * Auto-generated on create or when empty.
     */
    @Column(name = "api_key", length = 64)
    @Getter
    @Setter
    private String apiKey;

    /**
     * Username or identifier of the user who created this component.
     */
    @Column(name = "created_by", nullable = false, length = 255)
    @Getter
    @Setter
    private String createdBy;

    /**
     * Timestamp indicating when the component was created.
     */
    @Column(name = "created_date", nullable = false)
    @Getter
    @Setter
    private ZonedDateTime createdDate;

    /**
     * Username or identifier of the user who last modified this component.
     */
    @Column(name = "last_modified_by", nullable = false, length = 255)
    @Getter
    @Setter
    private String lastModifiedBy;

    /**
     * Timestamp indicating when the component was last modified.
     */
    @Column(name = "last_modified_date", nullable = false)
    @Getter
    @Setter
    private ZonedDateTime lastModifiedDate;

    /**
     * Technical timestamp in epoch milliseconds for when the component was created.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "created_date_technical", nullable = false)
    @Getter
    @Setter
    private Long createdDateTechnical;

    /**
     * Technical timestamp in epoch milliseconds for when the component was last modified.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "last_modified_date_technical", nullable = false)
    @Getter
    @Setter
    private Long lastModifiedDateTechnical;

    /**
     * Default constructor required by JPA.
     */
    public StatusComponent() {
    }
}
