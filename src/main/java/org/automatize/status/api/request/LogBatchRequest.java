package org.automatize.status.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request payload for bulk log ingestion in the status-monitoring app.
 *
 * <p>Wraps a non-empty, individually-validated list of {@link LogRequest} entries so that
 * multiple log records can be submitted in a single API call, improving throughput for
 * high-volume log producers.</p>
 */
public class LogBatchRequest {

    @NotEmpty(message = "Batch must contain at least one log entry")
    @Valid
    private List<LogRequest> logs;

    /**
     * Creates an empty log batch request for framework/deserialization use.
     */
    public LogBatchRequest() {
    }

    /** @return the list of log entries in this batch */
    public List<LogRequest> getLogs() { return logs; }
    /** @param logs the list of log entries to set */
    public void setLogs(List<LogRequest> logs) { this.logs = logs; }
}
