package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Entity representing a status application (service or platform) being monitored.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Track availability and health of monitored services</li>
 *   <li>Store health check configuration for automated monitoring</li>
 *   <li>Maintain relationships with components, incidents, and maintenance windows</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusComponent
 * @see StatusIncident
 * @see StatusMaintenance
 * @see Tenant
 * @see Organization
 */
@Entity
@Table(name = "status_apps")
@EntityListeners(AuditTimestampListener.class)
public class StatusApp implements Auditable {

    /**
     * Unique identifier for the status application.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    @Getter
    @Setter
    private UUID id;

    /**
     * Display name of the application or service being monitored.
     */
    @Column(name = "name", nullable = false, length = 255)
    @Getter
    @Setter
    private String name;

    /**
     * Detailed description of the application and its purpose.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    @Getter
    @Setter
    private String description;

    /**
     * URL-friendly slug identifier for the application.
     * Used in public status page URLs.
     */
    @Column(name = "slug", nullable = false, length = 255)
    @Getter
    @Setter
    private String slug;

    /**
     * Flag indicating whether the application's status is publicly visible.
     * Defaults to true for public status pages.
     */
    @Column(name = "is_public", nullable = false)
    @Getter
    @Setter
    private Boolean isPublic = true;

    /**
     * Current operational status of the application.
     * Defaults to "OPERATIONAL". Common values include OPERATIONAL, DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE.
     */
    @Column(name = "status", nullable = false, length = 50)
    @Getter
    @Setter
    private String status = "OPERATIONAL";

    /**
     * The tenant that owns this application.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @Getter
    @Setter
    private Tenant tenant;

    /**
     * The organization that owns this application.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @Getter
    @Setter
    private Organization organization;

    /**
     * The parent platform that this application belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id")
    @Getter
    @Setter
    private StatusPlatform platform;

    /**
     * Flag indicating whether automated health checks are enabled for this application.
     * Defaults to false.
     */
    @Column(name = "check_enabled")
    @Getter
    @Setter
    private Boolean checkEnabled = false;

    /**
     * Type of health check to perform.
     * Defaults to "NONE". Common values include NONE, HTTP, HTTPS, TCP.
     */
    @Column(name = "check_type", length = 50)
    @Getter
    @Setter
    private String checkType = "NONE";

    /**
     * URL endpoint to check for health status.
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
     * Number of consecutive failures required before marking the application as down.
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
     * Username or identifier of the user who created this application.
     */
    @Column(name = "created_by", nullable = false, length = 255)
    @Getter
    @Setter
    private String createdBy;

    /**
     * Timestamp indicating when the application was created.
     */
    @Column(name = "created_date", nullable = false)
    @Getter
    @Setter
    private ZonedDateTime createdDate;

    /**
     * Username or identifier of the user who last modified this application.
     */
    @Column(name = "last_modified_by", nullable = false, length = 255)
    @Getter
    @Setter
    private String lastModifiedBy;

    /**
     * Timestamp indicating when the application was last modified.
     */
    @Column(name = "last_modified_date", nullable = false)
    @Getter
    @Setter
    private ZonedDateTime lastModifiedDate;

    /**
     * Technical timestamp in epoch milliseconds for when the application was created.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "created_date_technical", nullable = false)
    @Getter
    @Setter
    private Long createdDateTechnical;

    /**
     * Technical timestamp in epoch milliseconds for when the application was last modified.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "last_modified_date_technical", nullable = false)
    @Getter
    @Setter
    private Long lastModifiedDateTechnical;

    /**
     * Default constructor required by JPA.
     */
    public StatusApp() {
    }
}
