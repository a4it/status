package org.automatize.status.models;

import jakarta.persistence.*;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.JobStatus;
import org.automatize.status.models.scheduler.JobType;
import org.automatize.status.models.scheduler.StringListConverter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a scheduled job definition.
 *
 * <p>A job defines what work to perform (via job_type), when to perform it
 * (cron_expression), and how to handle failures (retry/timeout settings).
 * The type-specific configuration lives in one of the four config tables
 * (program, sql, rest, soap), linked via one-to-one relationships.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Entity
@Table(name = "scheduler_jobs")
public class SchedulerJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private JobType jobType;

    @Column(name = "cron_expression", nullable = false, length = 255)
    private String cronExpression;

    @Column(name = "time_zone", nullable = false, length = 100)
    private String timeZone = "UTC";

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "allow_concurrent", nullable = false)
    private Boolean allowConcurrent = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "last_run_at")
    private ZonedDateTime lastRunAt;

    @Column(name = "next_run_at")
    private ZonedDateTime nextRunAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_run_status", length = 50)
    private JobRunStatus lastRunStatus;

    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures = 0;

    @Column(name = "max_retry_attempts", nullable = false)
    private Integer maxRetryAttempts = 0;

    @Column(name = "retry_delay_seconds", nullable = false)
    private Integer retryDelaySeconds = 60;

    @Column(name = "timeout_seconds", nullable = false)
    private Integer timeoutSeconds = 300;

    @Column(name = "max_output_bytes", nullable = false)
    private Integer maxOutputBytes = 102400;

    @Convert(converter = StringListConverter.class)
    @Column(name = "tags", columnDefinition = "TEXT")
    private List<String> tags;

    // Audit fields
    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_date")
    private ZonedDateTime createdDate;

    @Column(name = "created_date_technical")
    private Long createdDateTechnical;

    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    @Column(name = "last_modified_date")
    private ZonedDateTime lastModifiedDate;

    @Column(name = "last_modified_date_technical")
    private Long lastModifiedDateTechnical;

    // Config relationships
    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SchedulerProgramConfig programConfig;

    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SchedulerSqlConfig sqlConfig;

    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SchedulerRestConfig restConfig;

    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SchedulerSoapConfig soapConfig;

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

    @PreUpdate
    public void preUpdate() {
        lastModifiedDate = ZonedDateTime.now();
        lastModifiedDateTechnical = System.currentTimeMillis();
    }

    public SchedulerJob() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getAllowConcurrent() {
        return allowConcurrent;
    }

    public void setAllowConcurrent(Boolean allowConcurrent) {
        this.allowConcurrent = allowConcurrent;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public ZonedDateTime getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(ZonedDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public ZonedDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(ZonedDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public JobRunStatus getLastRunStatus() {
        return lastRunStatus;
    }

    public void setLastRunStatus(JobRunStatus lastRunStatus) {
        this.lastRunStatus = lastRunStatus;
    }

    public Integer getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public Integer getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(Integer maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public Integer getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(Integer retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getMaxOutputBytes() {
        return maxOutputBytes;
    }

    public void setMaxOutputBytes(Integer maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
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

    public Long getCreatedDateTechnical() {
        return createdDateTechnical;
    }

    public void setCreatedDateTechnical(Long createdDateTechnical) {
        this.createdDateTechnical = createdDateTechnical;
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

    public Long getLastModifiedDateTechnical() {
        return lastModifiedDateTechnical;
    }

    public void setLastModifiedDateTechnical(Long lastModifiedDateTechnical) {
        this.lastModifiedDateTechnical = lastModifiedDateTechnical;
    }

    public SchedulerProgramConfig getProgramConfig() {
        return programConfig;
    }

    public void setProgramConfig(SchedulerProgramConfig programConfig) {
        this.programConfig = programConfig;
    }

    public SchedulerSqlConfig getSqlConfig() {
        return sqlConfig;
    }

    public void setSqlConfig(SchedulerSqlConfig sqlConfig) {
        this.sqlConfig = sqlConfig;
    }

    public SchedulerRestConfig getRestConfig() {
        return restConfig;
    }

    public void setRestConfig(SchedulerRestConfig restConfig) {
        this.restConfig = restConfig;
    }

    public SchedulerSoapConfig getSoapConfig() {
        return soapConfig;
    }

    public void setSoapConfig(SchedulerSoapConfig soapConfig) {
        this.soapConfig = soapConfig;
    }
}
