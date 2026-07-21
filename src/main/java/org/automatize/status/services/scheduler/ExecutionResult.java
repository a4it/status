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

    /**
     * Returns the terminal status of the execution.
     *
     * @return the run status
     */
    public JobRunStatus getStatus() {
        return status;
    }

    /**
     * Sets the terminal status of the execution.
     *
     * @param status the run status
     */
    public void setStatus(JobRunStatus status) {
        this.status = status;
    }

    /**
     * Returns the captured standard output (PROGRAM jobs).
     *
     * @return the stdout text
     */
    public String getStdoutOutput() {
        return stdoutOutput;
    }

    /**
     * Sets the captured standard output (PROGRAM jobs).
     *
     * @param stdoutOutput the stdout text
     */
    public void setStdoutOutput(String stdoutOutput) {
        this.stdoutOutput = stdoutOutput;
    }

    /**
     * Returns the captured standard error (PROGRAM jobs).
     *
     * @return the stderr text
     */
    public String getStderrOutput() {
        return stderrOutput;
    }

    /**
     * Sets the captured standard error (PROGRAM jobs).
     *
     * @param stderrOutput the stderr text
     */
    public void setStderrOutput(String stderrOutput) {
        this.stderrOutput = stderrOutput;
    }

    /**
     * Returns the process exit code (PROGRAM jobs).
     *
     * @return the exit code, or {@code null}
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * Sets the process exit code (PROGRAM jobs).
     *
     * @param exitCode the exit code
     */
    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * Returns the HTTP status code (REST/SOAP jobs).
     *
     * @return the HTTP status code, or {@code null}
     */
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * Sets the HTTP status code (REST/SOAP jobs).
     *
     * @param httpStatusCode the HTTP status code
     */
    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Returns the number of rows affected/returned (SQL jobs).
     *
     * @return the affected row count, or {@code null}
     */
    public Long getRowsAffected() {
        return rowsAffected;
    }

    /**
     * Sets the number of rows affected/returned (SQL jobs).
     *
     * @param rowsAffected the affected row count
     */
    public void setRowsAffected(Long rowsAffected) {
        this.rowsAffected = rowsAffected;
    }

    /**
     * Returns the response body (REST/SOAP jobs, or serialised SQL result set).
     *
     * @return the response body
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Sets the response body (REST/SOAP jobs, or serialised SQL result set).
     *
     * @param responseBody the response body
     */
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    /**
     * Returns the error message describing why the run failed, if any.
     *
     * @return the error message, or {@code null}
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message describing why the run failed.
     *
     * @param errorMessage the error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
