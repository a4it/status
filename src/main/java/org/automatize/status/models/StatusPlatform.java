package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing a status platform that groups multiple status applications.
 * <p>
 * StatusPlatform is a higher-level entity that can contain multiple StatusApp entities,
 * allowing for hierarchical organization of monitored services. For example, a platform
 * like "Atlassian Cloud" could contain multiple apps like "Jira", "Confluence", etc.
 * </p>
 * <p>
 * Platforms can have their own status, which may be derived from the aggregate status
 * of their child applications, and can be displayed on status pages as groupings.
 * </p>
 *
 * @see StatusApp
 * @see Tenant
 * @see Organization
 */
@Entity
@Table(name = "status_platforms")
public class StatusPlatform {

    /**
     * Unique identifier for the platform.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Display name of the platform.
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Detailed description of the platform.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * URL-friendly slug identifier for the platform.
     * Used in public status page URLs.
     */
    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    /**
     * URL to the platform logo image.
     */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /**
     * External website URL for the platform.
     */
    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    /**
     * Current operational status of the platform.
     * Defaults to "OPERATIONAL". Common values include OPERATIONAL, DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE.
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status = "OPERATIONAL";

    /**
     * Flag indicating whether the platform is publicly visible.
     * Defaults to true for public status pages.
     */
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    /**
     * Display position for ordering platforms.
     * Lower values appear first. Defaults to 0.
     */
    @Column(name = "position")
    private Integer position = 0;

    /**
     * The tenant that owns this platform.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    /**
     * The organization that owns this platform.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /**
     * Flag indicating whether automated health checks are enabled for this platform.
     * Defaults to false.
     */
    @Column(name = "check_enabled")
    private Boolean checkEnabled = false;

    /**
     * Type of health check to perform.
     * Defaults to "NONE". Common values include NONE, HTTP, HTTPS, TCP.
     */
    @Column(name = "check_type", length = 50)
    private String checkType = "NONE";

    /**
     * URL endpoint to check for health status.
     * Used when check type is HTTP or HTTPS.
     */
    @Column(name = "check_url", length = 500)
    private String checkUrl;

    /**
     * Interval in seconds between health check executions.
     * Defaults to 60 seconds.
     */
    @Column(name = "check_interval_seconds")
    private Integer checkIntervalSeconds = 60;

    /**
     * Timeout in seconds for health check requests.
     * Defaults to 10 seconds.
     */
    @Column(name = "check_timeout_seconds")
    private Integer checkTimeoutSeconds = 10;

    /**
     * Expected HTTP status code for successful health checks.
     * Defaults to 200.
     */
    @Column(name = "check_expected_status")
    private Integer checkExpectedStatus = 200;

    /**
     * Number of consecutive failures required before marking the platform as down.
     * Defaults to 3 to prevent false positives from transient issues.
     */
    @Column(name = "check_failure_threshold")
    private Integer checkFailureThreshold = 3;

    /**
     * Timestamp of the last health check execution.
     */
    @Column(name = "last_check_at")
    private ZonedDateTime lastCheckAt;

    /**
     * Flag indicating whether the last health check was successful.
     */
    @Column(name = "last_check_success")
    private Boolean lastCheckSuccess;

    /**
     * Message from the last health check providing details about the result.
     */
    @Column(name = "last_check_message", length = 1000)
    private String lastCheckMessage;

    /**
     * Count of consecutive failed health checks.
     * Resets to 0 on successful check.
     */
    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures = 0;

    /**
     * Username or identifier of the user who created this platform.
     */
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    /**
     * Timestamp indicating when the platform was created.
     */
    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    /**
     * Username or identifier of the user who last modified this platform.
     */
    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    /**
     * Timestamp indicating when the platform was last modified.
     */
    @Column(name = "last_modified_date", nullable = false)
    private ZonedDateTime lastModifiedDate;

    /**
     * Technical timestamp in epoch milliseconds for when the platform was created.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * Technical timestamp in epoch milliseconds for when the platform was last modified.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "last_modified_date_technical", nullable = false)
    private Long lastModifiedDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new platform.
     * Automatically sets creation and modification timestamps if not already set.
     */
    @PrePersist
    public void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        if (createdDate == null) {
            createdDate = now;
        }
        if (lastModifiedDate == null) {
            lastModifiedDate = now;
        }
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
        if (lastModifiedDateTechnical == null) {
            lastModifiedDateTechnical = System.currentTimeMillis();
        }
    }

    /**
     * JPA lifecycle callback executed before updating an existing platform.
     * Automatically updates the modification timestamps.
     */
    @PreUpdate
    public void preUpdate() {
        lastModifiedDate = ZonedDateTime.now();
        lastModifiedDateTechnical = System.currentTimeMillis();
    }

    /**
     * Default constructor required by JPA.
     */
    public StatusPlatform() {
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

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Long getCreatedDateTechnical() {
        return createdDateTechnical;
    }

    public void setCreatedDateTechnical(Long createdDateTechnical) {
        this.createdDateTechnical = createdDateTechnical;
    }

    public Long getLastModifiedDateTechnical() {
        return lastModifiedDateTechnical;
    }

    public void setLastModifiedDateTechnical(Long lastModifiedDateTechnical) {
        this.lastModifiedDateTechnical = lastModifiedDateTechnical;
    }

    public Boolean getCheckEnabled() {
        return checkEnabled;
    }

    public void setCheckEnabled(Boolean checkEnabled) {
        this.checkEnabled = checkEnabled;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getCheckUrl() {
        return checkUrl;
    }

    public void setCheckUrl(String checkUrl) {
        this.checkUrl = checkUrl;
    }

    public Integer getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    public Integer getCheckTimeoutSeconds() {
        return checkTimeoutSeconds;
    }

    public void setCheckTimeoutSeconds(Integer checkTimeoutSeconds) {
        this.checkTimeoutSeconds = checkTimeoutSeconds;
    }

    public Integer getCheckExpectedStatus() {
        return checkExpectedStatus;
    }

    public void setCheckExpectedStatus(Integer checkExpectedStatus) {
        this.checkExpectedStatus = checkExpectedStatus;
    }

    public Integer getCheckFailureThreshold() {
        return checkFailureThreshold;
    }

    public void setCheckFailureThreshold(Integer checkFailureThreshold) {
        this.checkFailureThreshold = checkFailureThreshold;
    }

    public ZonedDateTime getLastCheckAt() {
        return lastCheckAt;
    }

    public void setLastCheckAt(ZonedDateTime lastCheckAt) {
        this.lastCheckAt = lastCheckAt;
    }

    public Boolean getLastCheckSuccess() {
        return lastCheckSuccess;
    }

    public void setLastCheckSuccess(Boolean lastCheckSuccess) {
        this.lastCheckSuccess = lastCheckSuccess;
    }

    public String getLastCheckMessage() {
        return lastCheckMessage;
    }

    public void setLastCheckMessage(String lastCheckMessage) {
        this.lastCheckMessage = lastCheckMessage;
    }

    public Integer getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }
}
