package org.automatize.status.api.response;

import java.util.List;

public class ProcessMiningResponse {

    private List<ProcessCase> cases;
    private int totalCases;
    private boolean truncated;

    public ProcessMiningResponse() {
    }

    public ProcessMiningResponse(List<ProcessCase> cases, int totalCases, boolean truncated) {
        this.cases = cases;
        this.totalCases = totalCases;
        this.truncated = truncated;
    }

    public List<ProcessCase> getCases() { return cases; }
    public void setCases(List<ProcessCase> cases) { this.cases = cases; }

    public int getTotalCases() { return totalCases; }
    public void setTotalCases(int totalCases) { this.totalCases = totalCases; }

    public boolean isTruncated() { return truncated; }
    public void setTruncated(boolean truncated) { this.truncated = truncated; }

    public static class ProcessCase {
        private String caseId;
        private List<ProcessEvent> events;

        public ProcessCase() {
        }

        public ProcessCase(String caseId, List<ProcessEvent> events) {
            this.caseId = caseId;
            this.events = events;
        }

        public String getCaseId() { return caseId; }
        public void setCaseId(String caseId) { this.caseId = caseId; }

        public List<ProcessEvent> getEvents() { return events; }
        public void setEvents(List<ProcessEvent> events) { this.events = events; }
    }

    public static class ProcessEvent {
        private String activity;
        private String timestamp;
        private String icon;
        private String level;
        private String message;

        public ProcessEvent() {
        }

        public ProcessEvent(String activity, String timestamp, String icon, String level, String message) {
            this.activity = activity;
            this.timestamp = timestamp;
            this.icon = icon;
            this.level = level;
            this.message = message;
        }

        public String getActivity() { return activity; }
        public void setActivity(String activity) { this.activity = activity; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
