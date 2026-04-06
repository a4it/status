package org.automatize.status.api.response;

import org.automatize.status.models.SchedulerJobRun;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Response class for a single job execution run record.
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
public class SchedulerJobRunResponse {

    private UUID id;
    private UUID jobId;
    private String jobName;
    private String triggerType;
    private String status;
    private Integer attemptNumber;
    private ZonedDateTime startedAt;
    private ZonedDateTime finishedAt;
    private Long durationMs;
    private String stdoutOutput;
    private String stderrOutput;
    private Integer exitCode;
    private Integer httpStatusCode;
    private Long rowsAffected;
    private String responseBody;
    private String errorMessage;
    private String triggeredBy;

    // -------------------------------------------------------------------------
    // Factory method
    // -------------------------------------------------------------------------

    public static SchedulerJobRunResponse fromEntity(SchedulerJobRun run) {
        SchedulerJobRunResponse r = new SchedulerJobRunResponse();
        r.id = run.getId();
        r.jobId = run.getJob() != null ? run.getJob().getId() : null;
        r.jobName = run.getJob() != null ? run.getJob().getName() : null;
        r.triggerType = run.getTriggerType() != null ? run.getTriggerType().name() : null;
        r.status = run.getStatus() != null ? run.getStatus().name() : null;
        r.attemptNumber = run.getAttemptNumber();
        r.startedAt = run.getStartedAt();
        r.finishedAt = run.getFinishedAt();
        r.durationMs = run.getDurationMs();
        r.stdoutOutput = run.getStdoutOutput();
        r.stderrOutput = run.getStderrOutput();
        r.exitCode = run.getExitCode();
        r.httpStatusCode = run.getHttpStatusCode();
        r.rowsAffected = run.getRowsAffected();
        r.responseBody = run.getResponseBody();
        r.errorMessage = run.getErrorMessage();
        r.triggeredBy = run.getTriggeredBy();
        return r;
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }

    public ZonedDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(ZonedDateTime startedAt) { this.startedAt = startedAt; }

    public ZonedDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(ZonedDateTime finishedAt) { this.finishedAt = finishedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getStdoutOutput() { return stdoutOutput; }
    public void setStdoutOutput(String stdoutOutput) { this.stdoutOutput = stdoutOutput; }

    public String getStderrOutput() { return stderrOutput; }
    public void setStderrOutput(String stderrOutput) { this.stderrOutput = stderrOutput; }

    public Integer getExitCode() { return exitCode; }
    public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }

    public Integer getHttpStatusCode() { return httpStatusCode; }
    public void setHttpStatusCode(Integer httpStatusCode) { this.httpStatusCode = httpStatusCode; }

    public Long getRowsAffected() { return rowsAffected; }
    public void setRowsAffected(Long rowsAffected) { this.rowsAffected = rowsAffected; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
}
