package org.automatize.status.models;

import jakarta.persistence.*;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.JobStatus;
import org.automatize.status.models.scheduler.JobType;
import org.automatize.status.models.scheduler.StringListConverter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

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
@EntityListeners(AuditTimestampListener.class)
public class SchedulerJob implements Auditable {

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
    @Getter
    @Setter
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    @Getter
    @Setter
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

    /**
     * Default constructor required by JPA.
     */
    public SchedulerJob() {
    }

    /**
     * Gets the unique identifier of the job.
     *
     * @return the UUID of the job
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the job.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the tenant that owns this job.
     *
     * @return the associated {@link Tenant}
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Sets the tenant that owns this job.
     *
     * @param tenant the {@link Tenant} to associate
     */
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    /**
     * Gets the organisation this job is scoped to, if any.
     *
     * @return the associated {@link Organization}, or {@code null} if tenant-wide
     */
    public Organization getOrganization() {
        return organization;
    }

    /**
     * Sets the organisation this job is scoped to.
     *
     * @param organization the {@link Organization} to associate, or {@code null} for tenant-wide
     */
    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    /**
     * Gets the type of work this job performs.
     *
     * @return the {@link JobType}
     */
    public JobType getJobType() {
        return jobType;
    }

    /**
     * Sets the type of work this job performs.
     *
     * @param jobType the {@link JobType} to set
     */
    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    /**
     * Gets the cron expression that defines the job's schedule.
     *
     * @return the cron expression
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * Sets the cron expression that defines the job's schedule.
     *
     * @param cronExpression the cron expression to set
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * Gets the time zone used to evaluate the cron expression.
     *
     * @return the time zone identifier
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Sets the time zone used to evaluate the cron expression.
     *
     * @param timeZone the time zone identifier to set
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Indicates whether this job is enabled for scheduling.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets whether this job is enabled for scheduling.
     *
     * @param enabled the enabled flag to set
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Indicates whether concurrent executions of this job are allowed.
     *
     * @return {@code true} if concurrent runs are permitted, {@code false} otherwise
     */
    public Boolean getAllowConcurrent() {
        return allowConcurrent;
    }

    /**
     * Sets whether concurrent executions of this job are allowed.
     *
     * @param allowConcurrent the allow-concurrent flag to set
     */
    public void setAllowConcurrent(Boolean allowConcurrent) {
        this.allowConcurrent = allowConcurrent;
    }

    /**
     * Gets the current lifecycle status of the job.
     *
     * @return the {@link JobStatus}
     */
    public JobStatus getStatus() {
        return status;
    }

    /**
     * Sets the current lifecycle status of the job.
     *
     * @param status the {@link JobStatus} to set
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * Gets the timestamp of the most recent execution.
     *
     * @return the last run time, or {@code null} if never run
     */
    public ZonedDateTime getLastRunAt() {
        return lastRunAt;
    }

    /**
     * Sets the timestamp of the most recent execution.
     *
     * @param lastRunAt the last run time to set
     */
    public void setLastRunAt(ZonedDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    /**
     * Gets the timestamp of the next scheduled execution.
     *
     * @return the next run time, or {@code null} if not scheduled
     */
    public ZonedDateTime getNextRunAt() {
        return nextRunAt;
    }

    /**
     * Sets the timestamp of the next scheduled execution.
     *
     * @param nextRunAt the next run time to set
     */
    public void setNextRunAt(ZonedDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    /**
     * Gets the status of the most recent execution.
     *
     * @return the {@link JobRunStatus} of the last run, or {@code null} if never run
     */
    public JobRunStatus getLastRunStatus() {
        return lastRunStatus;
    }

    /**
     * Sets the status of the most recent execution.
     *
     * @param lastRunStatus the {@link JobRunStatus} to set
     */
    public void setLastRunStatus(JobRunStatus lastRunStatus) {
        this.lastRunStatus = lastRunStatus;
    }

    /**
     * Gets the number of consecutive failed executions.
     *
     * @return the consecutive failure count
     */
    public Integer getConsecutiveFailures() {
        return consecutiveFailures;
    }

    /**
     * Sets the number of consecutive failed executions.
     *
     * @param consecutiveFailures the consecutive failure count to set
     */
    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    /**
     * Gets the maximum number of retry attempts on failure.
     *
     * @return the maximum retry attempts
     */
    public Integer getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    /**
     * Sets the maximum number of retry attempts on failure.
     *
     * @param maxRetryAttempts the maximum retry attempts to set
     */
    public void setMaxRetryAttempts(Integer maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    /**
     * Gets the delay in seconds between retry attempts.
     *
     * @return the retry delay in seconds
     */
    public Integer getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    /**
     * Sets the delay in seconds between retry attempts.
     *
     * @param retryDelaySeconds the retry delay in seconds to set
     */
    public void setRetryDelaySeconds(Integer retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    /**
     * Gets the maximum execution time in seconds before the job times out.
     *
     * @return the timeout in seconds
     */
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Sets the maximum execution time in seconds before the job times out.
     *
     * @param timeoutSeconds the timeout in seconds to set
     */
    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Gets the maximum number of output bytes captured from a run.
     *
     * @return the maximum output size in bytes
     */
    public Integer getMaxOutputBytes() {
        return maxOutputBytes;
    }

    /**
     * Sets the maximum number of output bytes captured from a run.
     *
     * @param maxOutputBytes the maximum output size in bytes to set
     */
    public void setMaxOutputBytes(Integer maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }

    /**
     * Gets the list of tags associated with the job.
     *
     * @return the list of tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Sets the list of tags associated with the job.
     *
     * @param tags the list of tags to set
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * Gets the username of the user who created this job.
     *
     * @return the creator's username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username of the user who created this job.
     *
     * @param createdBy the creator's username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the creation timestamp of the job.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation timestamp of the job.
     *
     * @param createdDate the creation date and time to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the technical creation timestamp in epoch milliseconds.
     *
     * @return the creation timestamp in milliseconds since epoch
     */
    public Long getCreatedDateTechnical() {
        return createdDateTechnical;
    }

    /**
     * Sets the technical creation timestamp in epoch milliseconds.
     *
     * @param createdDateTechnical the creation timestamp in milliseconds to set
     */
    public void setCreatedDateTechnical(Long createdDateTechnical) {
        this.createdDateTechnical = createdDateTechnical;
    }

    /**
     * Gets the username of the user who last modified this job.
     *
     * @return the last modifier's username
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the username of the user who last modified this job.
     *
     * @param lastModifiedBy the last modifier's username to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Gets the last modification timestamp of the job.
     *
     * @return the last modification date and time
     */
    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modification timestamp of the job.
     *
     * @param lastModifiedDate the last modification date and time to set
     */
    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Gets the technical last modification timestamp in epoch milliseconds.
     *
     * @return the last modification timestamp in milliseconds since epoch
     */
    public Long getLastModifiedDateTechnical() {
        return lastModifiedDateTechnical;
    }

    /**
     * Sets the technical last modification timestamp in epoch milliseconds.
     *
     * @param lastModifiedDateTechnical the last modification timestamp in milliseconds to set
     */
    public void setLastModifiedDateTechnical(Long lastModifiedDateTechnical) {
        this.lastModifiedDateTechnical = lastModifiedDateTechnical;
    }

    /**
     * Gets the program-type configuration for this job, if applicable.
     *
     * @return the {@link SchedulerProgramConfig}, or {@code null}
     */
    public SchedulerProgramConfig getProgramConfig() {
        return programConfig;
    }

    /**
     * Sets the program-type configuration for this job.
     *
     * @param programConfig the {@link SchedulerProgramConfig} to set
     */
    public void setProgramConfig(SchedulerProgramConfig programConfig) {
        this.programConfig = programConfig;
    }

    /**
     * Gets the SQL-type configuration for this job, if applicable.
     *
     * @return the {@link SchedulerSqlConfig}, or {@code null}
     */
    public SchedulerSqlConfig getSqlConfig() {
        return sqlConfig;
    }

    /**
     * Sets the SQL-type configuration for this job.
     *
     * @param sqlConfig the {@link SchedulerSqlConfig} to set
     */
    public void setSqlConfig(SchedulerSqlConfig sqlConfig) {
        this.sqlConfig = sqlConfig;
    }

    /**
     * Gets the REST-type configuration for this job, if applicable.
     *
     * @return the {@link SchedulerRestConfig}, or {@code null}
     */
    public SchedulerRestConfig getRestConfig() {
        return restConfig;
    }

    /**
     * Sets the REST-type configuration for this job.
     *
     * @param restConfig the {@link SchedulerRestConfig} to set
     */
    public void setRestConfig(SchedulerRestConfig restConfig) {
        this.restConfig = restConfig;
    }

    /**
     * Gets the SOAP-type configuration for this job, if applicable.
     *
     * @return the {@link SchedulerSoapConfig}, or {@code null}
     */
    public SchedulerSoapConfig getSoapConfig() {
        return soapConfig;
    }

    /**
     * Sets the SOAP-type configuration for this job.
     *
     * @param soapConfig the {@link SchedulerSoapConfig} to set
     */
    public void setSoapConfig(SchedulerSoapConfig soapConfig) {
        this.soapConfig = soapConfig;
    }
}
