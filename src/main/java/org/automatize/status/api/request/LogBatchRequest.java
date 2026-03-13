package org.automatize.status.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class LogBatchRequest {

    @NotEmpty(message = "Batch must contain at least one log entry")
    @Valid
    private List<LogRequest> logs;

    public LogBatchRequest() {
    }

    public List<LogRequest> getLogs() { return logs; }
    public void setLogs(List<LogRequest> logs) { this.logs = logs; }
}
