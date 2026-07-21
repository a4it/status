package org.automatize.status.api.response;

import java.util.List;

/**
 * <p>
 * Response object carrying the result of a process-mining query, grouping
 * monitored activity into per-case event traces for the status-monitoring
 * application.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Expose the discovered process cases and their ordered events</li>
 *   <li>Report the total number of matching cases</li>
 *   <li>Signal whether the returned result set was truncated</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
public class ProcessMiningResponse {

    private List<ProcessCase> cases;
    private int totalCases;
    private boolean truncated;

    /**
     * Default constructor.
     */
    public ProcessMiningResponse() {
    }

    /**
     * Creates a fully populated process-mining response.
     *
     * @param cases      the discovered process cases
     * @param totalCases the total number of matching cases
     * @param truncated  whether the returned result set was truncated
     */
    public ProcessMiningResponse(List<ProcessCase> cases, int totalCases, boolean truncated) {
        this.cases = cases;
        this.totalCases = totalCases;
        this.truncated = truncated;
    }

    /** Gets the process cases. @return the list of process cases */
    public List<ProcessCase> getCases() { return cases; }
    /** Sets the process cases. @param cases the list of process cases to set */
    public void setCases(List<ProcessCase> cases) { this.cases = cases; }

    /** Gets the total number of matching cases. @return the total case count */
    public int getTotalCases() { return totalCases; }
    /** Sets the total number of matching cases. @param totalCases the total case count to set */
    public void setTotalCases(int totalCases) { this.totalCases = totalCases; }

    /** Gets whether the result set was truncated. @return {@code true} if truncated */
    public boolean isTruncated() { return truncated; }
    /** Sets whether the result set was truncated. @param truncated the truncation flag to set */
    public void setTruncated(boolean truncated) { this.truncated = truncated; }

    /**
     * <p>
     * Represents a single process case: an identified trace grouping together
     * the ordered sequence of events that belong to the same process instance.
     * </p>
     */
    public static class ProcessCase {
        private String caseId;
        private List<ProcessEvent> events;

        /**
         * Default constructor.
         */
        public ProcessCase() {
        }

        /**
         * Creates a fully populated process case.
         *
         * @param caseId the identifier of the process case
         * @param events the ordered events belonging to this case
         */
        public ProcessCase(String caseId, List<ProcessEvent> events) {
            this.caseId = caseId;
            this.events = events;
        }

        /** Gets the case identifier. @return the case id */
        public String getCaseId() { return caseId; }
        /** Sets the case identifier. @param caseId the case id to set */
        public void setCaseId(String caseId) { this.caseId = caseId; }

        /** Gets the events of this case. @return the ordered list of events */
        public List<ProcessEvent> getEvents() { return events; }
        /** Sets the events of this case. @param events the ordered list of events to set */
        public void setEvents(List<ProcessEvent> events) { this.events = events; }
    }

    /**
     * <p>
     * Represents a single event within a process case, describing one activity
     * step along with its timing, display icon, severity level and message.
     * </p>
     */
    public static class ProcessEvent {
        private String activity;
        private String timestamp;
        private String icon;
        private String level;
        private String message;

        /**
         * Default constructor.
         */
        public ProcessEvent() {
        }

        /**
         * Creates a fully populated process event.
         *
         * @param activity  the name of the activity step
         * @param timestamp the timestamp of the event
         * @param icon      the display icon for the event
         * @param level     the severity level of the event
         * @param message   the human-readable message describing the event
         */
        public ProcessEvent(String activity, String timestamp, String icon, String level, String message) {
            this.activity = activity;
            this.timestamp = timestamp;
            this.icon = icon;
            this.level = level;
            this.message = message;
        }

        /** Gets the activity name. @return the activity name */
        public String getActivity() { return activity; }
        /** Sets the activity name. @param activity the activity name to set */
        public void setActivity(String activity) { this.activity = activity; }

        /** Gets the event timestamp. @return the timestamp */
        public String getTimestamp() { return timestamp; }
        /** Sets the event timestamp. @param timestamp the timestamp to set */
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        /** Gets the display icon. @return the icon */
        public String getIcon() { return icon; }
        /** Sets the display icon. @param icon the icon to set */
        public void setIcon(String icon) { this.icon = icon; }

        /** Gets the severity level. @return the level */
        public String getLevel() { return level; }
        /** Sets the severity level. @param level the level to set */
        public void setLevel(String level) { this.level = level; }

        /** Gets the event message. @return the message */
        public String getMessage() { return message; }
        /** Sets the event message. @param message the message to set */
        public void setMessage(String message) { this.message = message; }
    }
}
