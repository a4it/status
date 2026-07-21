package org.automatize.status.api.request;

/**
 * Request payload for changing a log level in the status-monitoring app.
 *
 * <p>Carries a single target severity level, used to adjust the effective logging/filtering
 * threshold (for example the minimum level retained or reported).</p>
 */
public class LogLevelRequest {

    private String level;

    /**
     * Creates an empty log level request for framework/deserialization use.
     */
    public LogLevelRequest() {
    }

    /** @return the target log level */
    public String getLevel() { return level; }
    /** @param level the target log level to set */
    public void setLevel(String level) { this.level = level; }
}
