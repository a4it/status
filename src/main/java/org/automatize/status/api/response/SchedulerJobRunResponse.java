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

    /**
     * Builds a response from a {@link SchedulerJobRun} entity, flattening the
     * associated job reference and enum values into plain fields.
     *
     * @param run the scheduler job run entity to convert
     * @return a populated response instance
     */
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

    /** Gets the run ID. @return the run ID */
    public UUID getId() { return id; }
    /** Sets the run ID. @param id the run ID to set */
    public void setId(UUID id) { this.id = id; }

    /** Gets the parent job ID. @return the job ID */
    public UUID getJobId() { return jobId; }
    /** Sets the parent job ID. @param jobId the job ID to set */
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    /** Gets the job name. @return the job name */
    public String getJobName() { return jobName; }
    /** Sets the job name. @param jobName the job name to set */
    public void setJobName(String jobName) { this.jobName = jobName; }

    /** Gets the trigger type. @return the trigger type */
    public String getTriggerType() { return triggerType; }
    /** Sets the trigger type. @param triggerType the trigger type to set */
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    /** Gets the run status. @return the run status */
    public String getStatus() { return status; }
    /** Sets the run status. @param status the run status to set */
    public void setStatus(String status) { this.status = status; }

    /** Gets the attempt number. @return the attempt number */
    public Integer getAttemptNumber() { return attemptNumber; }
    /** Sets the attempt number. @param attemptNumber the attempt number to set */
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }

    /** Gets the start timestamp. @return the start timestamp */
    public ZonedDateTime getStartedAt() { return startedAt; }
    /** Sets the start timestamp. @param startedAt the start timestamp to set */
    public void setStartedAt(ZonedDateTime startedAt) { this.startedAt = startedAt; }

    /** Gets the finish timestamp. @return the finish timestamp */
    public ZonedDateTime getFinishedAt() { return finishedAt; }
    /** Sets the finish timestamp. @param finishedAt the finish timestamp to set */
    public void setFinishedAt(ZonedDateTime finishedAt) { this.finishedAt = finishedAt; }

    /** Gets the run duration in milliseconds. @return the duration in milliseconds */
    public Long getDurationMs() { return durationMs; }
    /** Sets the run duration in milliseconds. @param durationMs the duration to set */
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    /** Gets the captured standard output. @return the standard output */
    public String getStdoutOutput() { return stdoutOutput; }
    /** Sets the captured standard output. @param stdoutOutput the standard output to set */
    public void setStdoutOutput(String stdoutOutput) { this.stdoutOutput = stdoutOutput; }

    /** Gets the captured standard error output. @return the standard error output */
    public String getStderrOutput() { return stderrOutput; }
    /** Sets the captured standard error output. @param stderrOutput the standard error output to set */
    public void setStderrOutput(String stderrOutput) { this.stderrOutput = stderrOutput; }

    /** Gets the process exit code. @return the exit code */
    public Integer getExitCode() { return exitCode; }
    /** Sets the process exit code. @param exitCode the exit code to set */
    public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }

    /** Gets the HTTP status code. @return the HTTP status code */
    public Integer getHttpStatusCode() { return httpStatusCode; }
    /** Sets the HTTP status code. @param httpStatusCode the HTTP status code to set */
    public void setHttpStatusCode(Integer httpStatusCode) { this.httpStatusCode = httpStatusCode; }

    /** Gets the number of rows affected. @return the rows affected */
    public Long getRowsAffected() { return rowsAffected; }
    /** Sets the number of rows affected. @param rowsAffected the rows affected to set */
    public void setRowsAffected(Long rowsAffected) { this.rowsAffected = rowsAffected; }

    /** Gets the response body. @return the response body */
    public String getResponseBody() { return responseBody; }
    /** Sets the response body. @param responseBody the response body to set */
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    /** Gets the error message. @return the error message */
    public String getErrorMessage() { return errorMessage; }
    /** Sets the error message. @param errorMessage the error message to set */
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    /** Gets the identifier of who triggered the run. @return the triggering actor */
    public String getTriggeredBy() { return triggeredBy; }
    /** Sets the identifier of who triggered the run. @param triggeredBy the triggering actor to set */
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
}
