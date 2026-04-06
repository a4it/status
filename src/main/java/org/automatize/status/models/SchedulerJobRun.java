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

    public SchedulerJobRun() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SchedulerJob getJob() {
        return job;
    }

    public void setJob(SchedulerJob job) {
        this.job = job;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public JobTriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(JobTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public JobRunStatus getStatus() {
        return status;
    }

    public void setStatus(JobRunStatus status) {
        this.status = status;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public ZonedDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(ZonedDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getStdoutOutput() {
        return stdoutOutput;
    }

    public void setStdoutOutput(String stdoutOutput) {
        this.stdoutOutput = stdoutOutput;
    }

    public String getStderrOutput() {
        return stderrOutput;
    }

    public void setStderrOutput(String stderrOutput) {
        this.stderrOutput = stderrOutput;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public Long getRowsAffected() {
        return rowsAffected;
    }

    public void setRowsAffected(Long rowsAffected) {
        this.rowsAffected = rowsAffected;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public Long getCreatedDateTechnical() {
        return createdDateTechnical;
    }

    public void setCreatedDateTechnical(Long createdDateTechnical) {
        this.createdDateTechnical = createdDateTechnical;
    }
}
