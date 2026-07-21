package org.automatize.status.models;

import jakarta.persistence.*;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.JobTriggerType;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity recording a single execution run of a {@link SchedulerJob}.
 *
 * <p>Run records are written and updated by the dispatcher service; no
 * {@code @PrePersist} or {@code @PreUpdate} lifecycle callbacks are used —
 * all timestamps must be set explicitly by the calling code.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Entity
@Table(name = "scheduler_job_runs")
public class SchedulerJobRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private SchedulerJob job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", length = 50)
    private JobTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private JobRunStatus status;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;

    @Column(name = "started_at")
    private ZonedDateTime startedAt;

    @Column(name = "finished_at")
    private ZonedDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "stdout_output", columnDefinition = "TEXT")
    private String stdoutOutput;

    @Column(name = "stderr_output", columnDefinition = "TEXT")
    private String stderrOutput;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "rows_affected")
    private Long rowsAffected;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "triggered_by", length = 255)
    private String triggeredBy;

    @Column(name = "created_date_technical")
    private Long createdDateTechnical;

    /**
     * Default constructor required by JPA.
     */
    public SchedulerJobRun() {
    }

    /**
     * Gets the unique identifier of the run.
     *
     * @return the UUID of the run
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the run.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the job this run belongs to.
     *
     * @return the associated {@link SchedulerJob}
     */
    public SchedulerJob getJob() {
        return job;
    }

    /**
     * Sets the job this run belongs to.
     *
     * @param job the {@link SchedulerJob} to associate
     */
    public void setJob(SchedulerJob job) {
        this.job = job;
    }

    /**
     * Gets the tenant that owns this run.
     *
     * @return the associated {@link Tenant}
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Sets the tenant that owns this run.
     *
     * @param tenant the {@link Tenant} to associate
     */
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    /**
     * Gets the trigger type that initiated this run.
     *
     * @return the {@link JobTriggerType}
     */
    public JobTriggerType getTriggerType() {
        return triggerType;
    }

    /**
     * Sets the trigger type that initiated this run.
     *
     * @param triggerType the {@link JobTriggerType} to set
     */
    public void setTriggerType(JobTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    /**
     * Gets the current status of this run.
     *
     * @return the {@link JobRunStatus}
     */
    public JobRunStatus getStatus() {
        return status;
    }

    /**
     * Sets the current status of this run.
     *
     * @param status the {@link JobRunStatus} to set
     */
    public void setStatus(JobRunStatus status) {
        this.status = status;
    }

    /**
     * Gets the attempt number for this run (starting at 1).
     *
     * @return the attempt number
     */
    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    /**
     * Sets the attempt number for this run.
     *
     * @param attemptNumber the attempt number to set
     */
    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    /**
     * Gets the timestamp when this run started.
     *
     * @return the start time, or {@code null} if not yet started
     */
    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    /**
     * Sets the timestamp when this run started.
     *
     * @param startedAt the start time to set
     */
    public void setStartedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * Gets the timestamp when this run finished.
     *
     * @return the finish time, or {@code null} if not yet finished
     */
    public ZonedDateTime getFinishedAt() {
        return finishedAt;
    }

    /**
     * Sets the timestamp when this run finished.
     *
     * @param finishedAt the finish time to set
     */
    public void setFinishedAt(ZonedDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    /**
     * Gets the total execution duration in milliseconds.
     *
     * @return the duration in milliseconds, or {@code null} if not measured
     */
    public Long getDurationMs() {
        return durationMs;
    }

    /**
     * Sets the total execution duration in milliseconds.
     *
     * @param durationMs the duration in milliseconds to set
     */
    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * Gets the captured standard output of the run.
     *
     * @return the stdout content
     */
    public String getStdoutOutput() {
        return stdoutOutput;
    }

    /**
     * Sets the captured standard output of the run.
     *
     * @param stdoutOutput the stdout content to set
     */
    public void setStdoutOutput(String stdoutOutput) {
        this.stdoutOutput = stdoutOutput;
    }

    /**
     * Gets the captured standard error output of the run.
     *
     * @return the stderr content
     */
    public String getStderrOutput() {
        return stderrOutput;
    }

    /**
     * Sets the captured standard error output of the run.
     *
     * @param stderrOutput the stderr content to set
     */
    public void setStderrOutput(String stderrOutput) {
        this.stderrOutput = stderrOutput;
    }

    /**
     * Gets the process exit code for program-type runs.
     *
     * @return the exit code, or {@code null} if not applicable
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * Sets the process exit code for program-type runs.
     *
     * @param exitCode the exit code to set
     */
    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * Gets the HTTP status code for REST/SOAP-type runs.
     *
     * @return the HTTP status code, or {@code null} if not applicable
     */
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * Sets the HTTP status code for REST/SOAP-type runs.
     *
     * @param httpStatusCode the HTTP status code to set
     */
    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Gets the number of rows affected for SQL-type runs.
     *
     * @return the affected row count, or {@code null} if not applicable
     */
    public Long getRowsAffected() {
        return rowsAffected;
    }

    /**
     * Sets the number of rows affected for SQL-type runs.
     *
     * @param rowsAffected the affected row count to set
     */
    public void setRowsAffected(Long rowsAffected) {
        this.rowsAffected = rowsAffected;
    }

    /**
     * Gets the captured response body for REST/SOAP-type runs.
     *
     * @return the response body content
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Sets the captured response body for REST/SOAP-type runs.
     *
     * @param responseBody the response body content to set
     */
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    /**
     * Gets the error message recorded for a failed run.
     *
     * @return the error message, or {@code null} if the run did not fail
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message recorded for a failed run.
     *
     * @param errorMessage the error message to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the identifier of the user or system that triggered this run.
     *
     * @return the triggering principal
     */
    public String getTriggeredBy() {
        return triggeredBy;
    }

    /**
     * Sets the identifier of the user or system that triggered this run.
     *
     * @param triggeredBy the triggering principal to set
     */
    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
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
}
