package org.automatize.status.services.scheduler;

import org.automatize.status.models.scheduler.JobRunStatus;

/**
 * Value object carrying the outcome of a single job execution attempt.
 *
 * <p>Executor services populate this object after running a job; the
 * dispatcher service then transfers the fields to the {@code SchedulerJobRun}
 * entity before persisting the final run record.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
public class ExecutionResult {

    private JobRunStatus status;
    private String stdoutOutput;
    private String stderrOutput;
    private Integer exitCode;
    private Integer httpStatusCode;
    private Long rowsAffected;
    private String responseBody;
    private String errorMessage;

    public JobRunStatus getStatus() {
        return status;
    }

    public void setStatus(JobRunStatus status) {
        this.status = status;
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
}
